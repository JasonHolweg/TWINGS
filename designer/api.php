<?php
/* =========================================================================
   TWINGS Library API — publish, browse and fetch community wings.

   Storage: PDO (MySQL on the server, SQLite for local development).
   Connection settings live in data/config.php (auto-generated with a
   SQLite fallback on first run; not web-accessible).
   ========================================================================= */
declare(strict_types=1);

header_remove('X-Powered-By');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

// Never leak a stack trace to the client; log server-side, return a clean 500.
set_exception_handler(function (Throwable $e): void {
    error_log('[twings] ' . $e->getMessage());
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
    }
    echo json_encode(['ok' => false, 'error' => 'server']);
});

const MAX_YAML_BYTES = 32768;
const MAX_YAML_LINES = 220;
const MAX_LINE_CHARS = 400;
const MAX_BODY_BYTES = 262144; // generous JSON envelope; yaml is bounded after decode
const PUBLISH_PER_DAY = 20;
const PUBLISH_PER_DAY_GLOBAL = 500; // backstop against IP-spoofed floods
const ID_CHARS = 'abcdefghijklmnopqrstuvwxyz0123456789';

// Cloudflare edge ranges (https://www.cloudflare.com/ips/); keep roughly in sync.
const CLOUDFLARE_RANGES = [
    '173.245.48.0/20', '103.21.244.0/22', '103.22.200.0/22', '103.31.4.0/22',
    '141.101.64.0/18', '108.162.192.0/18', '190.93.240.0/20', '188.114.96.0/20',
    '197.234.240.0/22', '198.41.128.0/17', '162.158.0.0/15', '104.16.0.0/13',
    '104.24.0.0/14', '172.64.0.0/13', '131.0.72.0/22',
    '2400:cb00::/32', '2606:4700::/32', '2803:f800::/32', '2405:b500::/32',
    '2405:8100::/32', '2a06:98c0::/29', '2c0f:f248::/32',
];

/* ---------------- bootstrap ---------------- */
// Storage lives outside the web root when TWINGS_DATA_DIR is set (recommended
// in production); otherwise a local ./data dir for development. A SQLite DB
// inside the web root is a dev-only convenience — never rely on it in prod.
$dataDir = getenv('TWINGS_DATA_DIR') ?: ($_SERVER['TWINGS_DATA_DIR'] ?? '') ?: __DIR__ . '/data';
if (!is_dir($dataDir) && !mkdir($dataDir, 0755, true)) {
    fail(500, 'storage');
}
$htaccess = $dataDir . '/.htaccess';
if (!file_exists($htaccess)) {
    file_put_contents($htaccess, "Require all denied\n");
}
$configFile = $dataDir . '/config.php';
if (!file_exists($configFile)) {
    // var_export escapes every value, so a data path with an apostrophe cannot
    // corrupt the generated file; write atomically so a half-written config is
    // never require()d by a concurrent request.
    $defaults = [
        'dsn' => 'sqlite:' . $dataDir . '/library.db',
        'user' => null,
        'pass' => null,
        'salt' => bin2hex(random_bytes(16)),
        'admin_token' => bin2hex(random_bytes(20)),
    ];
    $cfg = "<?php\nreturn " . var_export($defaults, true) . ";\n";
    $tmp = $configFile . '.' . bin2hex(random_bytes(6)) . '.tmp';
    if (file_put_contents($tmp, $cfg, LOCK_EX) === false || !@rename($tmp, $configFile)) {
        @unlink($tmp);
        if (!file_exists($configFile)) fail(500, 'storage');
    }
}
$config = require $configFile;

try {
    $pdo = new PDO($config['dsn'], $config['user'] ?? null, $config['pass'] ?? null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    if ($pdo->getAttribute(PDO::ATTR_DRIVER_NAME) === 'mysql') {
        $pdo->exec("SET NAMES utf8mb4"); // emoji/umlauts in names survive round-trips
    }
    $pdo->exec('CREATE TABLE IF NOT EXISTS wings (
        id VARCHAR(20) PRIMARY KEY,
        name VARCHAR(64) NOT NULL,
        slug VARCHAR(64) NOT NULL,
        creator VARCHAR(48) NOT NULL,
        description VARCHAR(320) NOT NULL,
        yaml TEXT NOT NULL,
        content_hash VARCHAR(64) NOT NULL DEFAULT \'\',
        downloads INTEGER NOT NULL DEFAULT 0,
        created BIGINT NOT NULL,
        ip_hash VARCHAR(64) NOT NULL
    )');
    $pdo->exec('CREATE TABLE IF NOT EXISTS publishes (
        ip_hash VARCHAR(64) NOT NULL,
        ts BIGINT NOT NULL
    )');
} catch (Throwable $e) {
    fail(500, 'db');
}

$action = $_GET['action'] ?? '';
switch ($action) {
    case 'publish': publish($pdo, $config); break;
    case 'get':     getYaml($pdo); break;
    case 'info':    info($pdo); break;
    case 'list':    listWings($pdo); break;
    case 'delete':  deleteWing($pdo, $config); break;
    default:        fail(404, 'unknown_action');
}

/* ---------------- helpers ---------------- */
function fail(int $code, string $error): never {
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => false, 'error' => $error]);
    exit;
}

function ok(array $payload): never {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => true] + $payload);
    exit;
}

function ipInCidr(string $ip, string $cidr): bool {
    [$net, $bits] = explode('/', $cidr);
    $ipBin = @inet_pton($ip);
    $netBin = @inet_pton($net);
    if ($ipBin === false || $netBin === false || strlen($ipBin) !== strlen($netBin)) return false;
    $bits = (int)$bits;
    $bytes = intdiv($bits, 8);
    $rem = $bits % 8;
    if ($bytes > 0 && strncmp($ipBin, $netBin, $bytes) !== 0) return false;
    if ($rem === 0) return true;
    $mask = 0xFF << (8 - $rem) & 0xFF;
    return (ord($ipBin[$bytes]) & $mask) === (ord($netBin[$bytes]) & $mask);
}

/**
 * The client identity for rate limiting. CF-Connecting-IP is only trusted when
 * the TCP peer is actually a Cloudflare edge — otherwise an attacker hitting the
 * origin directly could spoof the header and bypass every limit.
 */
function clientIpHash(array $config): string {
    $remote = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
    $ip = $remote;
    $cf = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? '';
    if ($cf !== '') {
        foreach (CLOUDFLARE_RANGES as $cidr) {
            if (ipInCidr($remote, $cidr)) { $ip = $cf; break; }
        }
    }
    return hash('sha256', $ip . '|' . $config['salt']);
}

/** Strips control characters; newlines only when $keepNewlines. */
function cleanText(string $s, bool $keepNewlines = false): string {
    $pattern = $keepNewlines ? '/[^\P{C}\n]+/u' : '/\p{C}+/u';
    $s = preg_replace($pattern, '', $s) ?? '';
    return trim($s);
}

function slugify(string $name): string {
    $s = mb_strtolower($name);
    $s = str_replace(['ä', 'ö', 'ü', 'ß'], ['ae', 'oe', 'ue', 'ss'], $s);
    $s = preg_replace('/\s+/', '_', $s) ?? '';
    $s = preg_replace('/[^a-z0-9_-]/', '', $s) ?? '';
    return substr($s, 0, 32) ?: 'wings';
}

function wingId(): string {
    $id = '';
    for ($i = 0; $i < 10; $i++) {
        $id .= ID_CHARS[random_int(0, strlen(ID_CHARS) - 1)];
    }
    return $id;
}

function validId(?string $id): string {
    if ($id === null || !preg_match('/^[a-z0-9]{4,20}$/', $id)) {
        fail(400, 'bad_id');
    }
    return $id;
}

/* ---------------- endpoints ---------------- */
function publish(PDO $pdo, array $config): never {
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') fail(405, 'method');
    // Read one byte past the ceiling so an oversized body is rejected cleanly
    // instead of being silently truncated into invalid JSON.
    $raw = file_get_contents('php://input', false, null, 0, MAX_BODY_BYTES + 1);
    if ($raw === false) fail(400, 'bad_json');
    if (strlen($raw) > MAX_BODY_BYTES) fail(413, 'too_large');
    $body = json_decode($raw, true);
    if (!is_array($body)) fail(400, 'bad_json');

    $name = cleanText((string)($body['name'] ?? ''));
    $creator = cleanText((string)($body['creator'] ?? ''));
    $description = cleanText((string)($body['description'] ?? ''), true);
    $yaml = str_replace("\r", '', (string)($body['yaml'] ?? ''));

    if (mb_strlen($name) < 2 || mb_strlen($name) > 40) fail(400, 'bad_name');
    if ($creator === '') $creator = 'Anonym';
    if (mb_strlen($creator) > 30) fail(400, 'bad_creator');
    if (mb_strlen($description) > 300) fail(400, 'bad_description');

    // structural wing-file checks; the plugin fully validates before install
    if (strlen($yaml) < 50 || strlen($yaml) > MAX_YAML_BYTES) fail(400, 'bad_yaml');
    if (!preg_match('/^Particles:/m', $yaml) || !preg_match('/^pattern:/m', $yaml)) fail(400, 'bad_yaml');
    $lines = explode("\n", $yaml);
    if (count($lines) > MAX_YAML_LINES) fail(400, 'bad_yaml');
    foreach ($lines as $line) {
        if (strlen($line) > MAX_LINE_CHARS) fail(400, 'bad_yaml');
    }

    $ipHash = clientIpHash($config);
    $now = time();

    // Idempotent publish: if this client already published identical content,
    // return the existing wing instead of creating a duplicate. This makes
    // repeated clicks on the publish button harmless and does not consume the
    // rate limit.
    $contentHash = hash('sha256', $name . "\0" . $creator . "\0" . $description . "\0" . $yaml);
    $st = $pdo->prepare('SELECT id FROM wings WHERE ip_hash = ? AND content_hash = ? LIMIT 1');
    $st->execute([$ipHash, $contentHash]);
    $existing = $st->fetch();
    if ($existing) {
        ok(['id' => $existing['id'], 'command' => '/twings install ' . $existing['id'], 'duplicate' => true]);
    }

    // rate limit: per client, plus a global backstop that survives IP spoofing
    $pdo->prepare('DELETE FROM publishes WHERE ts < ?')->execute([$now - 86400]);
    $st = $pdo->prepare('SELECT COUNT(*) AS n FROM publishes WHERE ip_hash = ?');
    $st->execute([$ipHash]);
    if ((int)$st->fetch()['n'] >= PUBLISH_PER_DAY) fail(429, 'rate_limited');
    $st = $pdo->query('SELECT COUNT(*) AS n FROM publishes');
    if ((int)$st->fetch()['n'] >= PUBLISH_PER_DAY_GLOBAL) fail(429, 'rate_limited');

    $id = wingId();
    $st = $pdo->prepare('SELECT 1 FROM wings WHERE id = ?');
    for ($i = 0; $i < 5; $i++) {
        $st->execute([$id]);
        if (!$st->fetch()) break;
        $id = wingId();
    }

    $pdo->prepare('INSERT INTO wings (id, name, slug, creator, description, yaml, content_hash, downloads, created, ip_hash)
                   VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)')
        ->execute([$id, $name, slugify($name), $creator, $description, $yaml, $contentHash, $now, $ipHash]);
    $pdo->prepare('INSERT INTO publishes (ip_hash, ts) VALUES (?, ?)')->execute([$ipHash, $now]);

    ok(['id' => $id, 'command' => '/twings install ' . $id]);
}

function getYaml(PDO $pdo): never {
    $id = validId($_GET['id'] ?? null);
    $st = $pdo->prepare('SELECT slug, yaml FROM wings WHERE id = ?');
    $st->execute([$id]);
    $row = $st->fetch();
    if (!$row) fail(404, 'not_found');
    if (($_GET['stat'] ?? '1') !== '0') {
        $pdo->prepare('UPDATE wings SET downloads = downloads + 1 WHERE id = ?')->execute([$id]);
    }
    header('Content-Type: text/plain; charset=utf-8');
    header('X-Wing-Slug: ' . $row['slug']);
    echo $row['yaml'];
    exit;
}

function info(PDO $pdo): never {
    $id = validId($_GET['id'] ?? null);
    $st = $pdo->prepare('SELECT id, name, slug, creator, description, downloads, created FROM wings WHERE id = ?');
    $st->execute([$id]);
    $row = $st->fetch();
    if (!$row) fail(404, 'not_found');
    $row['downloads'] = (int)$row['downloads'];
    $row['created'] = (int)$row['created'];
    ok(['wing' => $row]);
}

function listWings(PDO $pdo): never {
    $q = cleanText((string)($_GET['q'] ?? ''));
    $sort = ($_GET['sort'] ?? 'new') === 'popular' ? 'downloads DESC, created DESC' : 'created DESC';
    $limit = max(1, min(50, (int)($_GET['limit'] ?? 24)));
    $offset = max(0, min(10000, (int)($_GET['offset'] ?? 0)));

    $where = '';
    $params = [];
    if ($q !== '') {
        $like = '%' . addcslashes(mb_substr($q, 0, 60), '%_\\') . '%';
        $where = "WHERE name LIKE ? OR creator LIKE ?";
        $params = [$like, $like];
    }
    $st = $pdo->prepare("SELECT COUNT(*) AS n FROM wings $where");
    $st->execute($params);
    $total = (int)$st->fetch()['n'];

    $st = $pdo->prepare("SELECT id, name, creator, description, yaml, downloads, created
                         FROM wings $where ORDER BY $sort LIMIT $limit OFFSET $offset");
    $st->execute($params);
    $wings = [];
    foreach ($st->fetchAll() as $row) {
        $row['downloads'] = (int)$row['downloads'];
        $row['created'] = (int)$row['created'];
        $wings[] = $row;
    }
    ok(['total' => $total, 'wings' => $wings]);
}

function deleteWing(PDO $pdo, array $config): never {
    // POST only, token in a header — keeps the admin secret out of access logs,
    // browser history and Referer headers.
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') fail(405, 'method');
    $token = (string)($_SERVER['HTTP_X_ADMIN_TOKEN'] ?? '');
    if ($token === '' || !hash_equals((string)$config['admin_token'], $token)) fail(403, 'forbidden');
    $id = validId($_GET['id'] ?? null);
    $st = $pdo->prepare('DELETE FROM wings WHERE id = ?');
    $st->execute([$id]);
    ok(['deleted' => $st->rowCount() > 0]);
}

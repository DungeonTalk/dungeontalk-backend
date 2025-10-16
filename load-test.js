import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate, Trend, Counter } from 'k6/metrics';

// === Metrics
const loginSuccess = new Rate('login_success');
const apiSuccess = new Rate('api_success');
const loginDur = new Trend('login_duration');
const apiDur = new Trend('api_duration');
const totalReq = new Counter('total_requests');

export const options = {
  // setup()에서 응답 body가 필요하므로 전역 discard 비활성화
  // 대신 개별 함수에서 필요시 body를 버림
  discardResponseBodies: false,
  thresholds: {
    // 전체 실패율
    http_req_failed: ['rate<0.01'],
    // 태그별 응답시간 목표(포트폴리오에 "로그인/일반 API 분리 측정" 어필 포인트)
    'http_req_duration{endpoint:login}': ['p(95)<1500', 'p(99)<2500'],
    'http_req_duration{endpoint:chatrooms_list}': ['p(95)<500', 'p(99)<1000'], // 개선된 목표
    login_success: ['rate>0.98'],
    api_success: ['rate>0.98'],
  },
  scenarios: {
    // 로그인 부하 → 토큰 확보 성능
    login_ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '45s', target: 100 },
        { duration: '1m30s', target: 150 },
        { duration: '30s', target: 0 },
      ],
      exec: 'loginOnly',
      gracefulStop: '10s',
    },
    // API 부하 → 토큰 재사용하여 리스트 조회 (최적화)
    api_constant: {
      executor: 'constant-arrival-rate',
      rate: 100,          // 초당 100 req (증가)
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 100,
      maxVUs: 200,
      exec: 'apiOnly',
      startTime: '45s',   // 로그인 부하가 올라간 뒤 시작
      gracefulStop: '10s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERS = [
  { name: 'test_user_1', password: 'password123' },
  { name: 'test_user_2', password: 'password123' },
  { name: 'test_user_3', password: 'password123' },
];

// 간단한 랜덤 IP 생성(레이트리밋 회피용)
function randomIp() {
  return `${Math.floor(Math.random()*256)}.${Math.floor(Math.random()*256)}.${Math.floor(Math.random()*256)}.${Math.floor(Math.random()*256)}`;
}

// setup 단계에서 토큰 미리 발급
export function setup() {
  console.log(`\n🔧 Setup: Generating tokens for ${USERS.length} users...`);
  console.log(`📍 Target URL: ${BASE_URL}`);

  const tokens = [];
  for (let user of USERS) {
    const res = http.post(
      `${BASE_URL}/v1/auth/login`,
      JSON.stringify({ name: user.name, password: user.password }),
      { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': randomIp() } }
    );

    // 응답 상태 로깅
    if (res.status !== 200) {
      console.error(`❌ Login failed for ${user.name}: HTTP ${res.status}`);
      console.error(`   Response: ${res.body ? res.body.substring(0, 200) : 'empty'}`);
      continue;
    }

    try {
      const data = JSON.parse(res.body);
      const token = data.data?.accessToken;

      if (token) {
        tokens.push({ username: user.name, token: token });
        console.log(`✅ Token generated for ${user.name}`);
      } else {
        console.error(`❌ No token in response for ${user.name}`);
        console.error(`   Response structure: ${JSON.stringify(data).substring(0, 200)}`);
      }
    } catch (e) {
      console.error(`❌ Failed to parse response for ${user.name}: ${e.message}`);
      console.error(`   Response body: ${res.body ? res.body.substring(0, 200) : 'empty'}`);
    }
  }

  if (tokens.length === 0) {
    console.error('\n🚨 FATAL: No tokens generated! Cannot proceed with test.');
    console.error('   Please check:');
    console.error('   1. Application is running at ' + BASE_URL);
    console.error('   2. Test users exist in database');
    console.error('   3. Login endpoint is accessible\n');
    throw new Error('Setup failed: No tokens generated');
  }

  console.log(`\n✅ Setup complete: ${tokens.length}/${USERS.length} tokens generated\n`);
  return { tokens };
}

export function loginOnly() {
  const u = USERS[Math.floor(Math.random() * USERS.length)];
  const headers = {
    'Content-Type': 'application/json',
    'X-Forwarded-For': randomIp(),
  };

  const start = Date.now();
  const res = http.post(
      `${BASE_URL}/v1/auth/login`,
      JSON.stringify({ name: u.name, password: u.password }),
      { headers, tags: { endpoint: 'login' } }
  );
  loginDur.add(Date.now() - start);
  totalReq.add(1);

  const ok = check(res, {
    'login 200': (r) => r.status === 200,
    'has accessToken': (r) => {
      try { return JSON.parse(r.body).data?.accessToken; } catch { return false; }
    },
  });
  loginSuccess.add(ok);

  sleep(0.2);
}

export function apiOnly(data) {
  // setup에서 받은 토큰 재사용 (로그인 불필요)
  if (!data || !data.tokens || data.tokens.length === 0) {
    console.error('No tokens available from setup');
    return;
  }

  const tokenData = data.tokens[Math.floor(Math.random() * data.tokens.length)];
  const ip = randomIp();

  // 인증 API 호출 (로그인 없이 바로 API 호출)
  const start = Date.now();
  const apiRes = http.get(
      `${BASE_URL}/v1/chat/room`,
      { headers: { Authorization: `Bearer ${tokenData.token}`, 'X-Forwarded-For': ip }, tags: { endpoint: 'chatrooms_list' } }
  );
  apiDur.add(Date.now() - start);
  totalReq.add(1);

  const ok = check(apiRes, { 'api 200': (r) => r.status === 200 });
  apiSuccess.add(ok);

  // 현실적인 think time
  sleep(0.1 + Math.random() * 0.2); // 짧게 조정
}

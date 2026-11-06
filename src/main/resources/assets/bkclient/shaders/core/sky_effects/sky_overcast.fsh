#version 150
uniform float uTime, uSpeed, uAlpha, uFov, uBrightness;
uniform vec2 uResolution;
uniform vec3 uColor, uSecondColor, uCamRight, uCamUp, uCamForward;
in vec2 vScreen;
out vec4 fragColor;
vec3 skyRay() {
 float f=tan(radians(uFov)*0.5);
 return normalize(uCamForward + uCamRight*vScreen.x*(uResolution.x/uResolution.y)*f + uCamUp*vScreen.y*f);
}
uniform float uFlash, uStormCell;
uniform vec3 CameraPosition;
#define time (uTime * uSpeed * 0.1)
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hash(float p) {
    return fract(sin(p * 91.3458) * 47453.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(
            mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
            mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
            u.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;

    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = mat2(1.62, 1.10, -1.10, 1.62) * p + vec2(18.7, 7.3);
        amplitude *= 0.52;
    }

    return value;
}


float fullSkyAmount(float skyHeight, float amount) {
    float lowerSky = 1.0 - smoothstep(-0.18, 0.18, skyHeight);
    float enabled = smoothstep(0.02, 0.18, amount);
    return clamp(mix(amount, max(amount, 0.97), lowerSky * enabled), 0.0, 1.0);
}

vec3 overcastSky(vec3 worldDirection) {
    float skyHeight = clamp(worldDirection.y * 0.5 + 0.5, 0.0, 1.0);
    vec2 cameraDrift = CameraPosition.xz * 0.0009;
    vec2 cloudUv = worldDirection.xz / max(0.28 + abs(worldDirection.y), 0.24);
    cloudUv += cameraDrift + vec2(time * 0.012, time * -0.009);

    float broadClouds = fbm(cloudUv * 1.55);
    float tornClouds = fbm(cloudUv * 4.15 + broadClouds * 1.70);
    float stormVeins = fbm(cloudUv * vec2(9.0, 2.1) + vec2(time * -0.04, time * 0.01));
    float cloudMass = smoothstep(0.24, 0.88, broadClouds * 0.78 + tornClouds * 0.50);
    float underside = 1.0 - smoothstep(0.48, 0.96, skyHeight);
    float horizonMist = 1.0 - smoothstep(0.02, 0.52, skyHeight);

    vec3 horizon = vec3(0.28, 0.31, 0.33);
    vec3 zenith = vec3(0.055, 0.065, 0.085);
    vec3 base = mix(horizon, zenith, smoothstep(0.0, 1.0, skyHeight));
    vec3 cloudLight = vec3(0.35, 0.37, 0.39);
    vec3 cloudDark = vec3(0.035, 0.040, 0.055);
    vec3 clouds = mix(cloudLight, cloudDark, cloudMass * (0.60 + underside * 0.48));

    float streakShadow = fbm(cloudUv * vec2(0.65, 2.4) + vec2(0.0, time * 0.035));
    vec3 sky = mix(base, clouds, 0.58 + cloudMass * 0.34);
    sky *= 0.58 + streakShadow * 0.13 - stormVeins * underside * 0.13;
    sky = mix(sky, vec3(0.30, 0.33, 0.35), horizonMist * 0.24);

    return sky;
}

float rainLayer(vec2 uv, float columns, float speed, float slant, float width, float length, float seed) {
    vec2 rainUv = uv;
    rainUv.x += rainUv.y * slant;
    rainUv.x += sin(time * 0.95 + rainUv.y * 6.0 + seed) * 0.008;
    rainUv.y -= time * speed;

    vec2 grid = vec2(columns, columns * 0.33);
    vec2 cell = floor(rainUv * grid);
    vec2 local = fract(rainUv * grid);
    float random = hash(cell + seed);
    float x = abs(local.x - random);
    float streak = 1.0 - smoothstep(width, width * 2.6, x);
    float body = smoothstep(0.0, length, local.y) * (1.0 - smoothstep(length, 1.0, local.y));
    float head = exp(-local.y * 9.0) * 0.55;
    float drop = streak * (body + head) * step(0.28, random);

    return drop;
}

float rainMist(vec2 uv) {
    float fine = fbm(vec2(uv.x * 24.0 + time * 0.4, uv.y * 5.0 - time * 1.2));
    return smoothstep(0.48, 1.0, fine) * (1.0 - smoothstep(0.70, 1.05, uv.y));
}


float lightningBolt(vec3 worldDirection, float pulse) {
    if (pulse <= 0.001 || worldDirection.y < 0.05) {
        return 0.0;
    }

    float stormCell = uStormCell;
    float anchor = mix(-0.62, 0.62, hash(stormCell + 3.1));
    vec2 uv = vec2(worldDirection.x - anchor, worldDirection.y - 0.18);
    uv.x += sin(uv.y * 28.0 + hash(stormCell + 9.2) * 6.2831) * 0.022;
    uv.x += sin(uv.y * 57.0 + time * 0.85) * 0.010;

    float mainBolt = (1.0 - smoothstep(0.006, 0.030, abs(uv.x))) * smoothstep(0.02, 0.22, uv.y)
            * (1.0 - smoothstep(0.76, 1.02, uv.y));
    float branchA = (1.0 - smoothstep(0.004, 0.022, abs(uv.x - uv.y * 0.34 + 0.13)))
            * smoothstep(0.20, 0.36, uv.y) * (1.0 - smoothstep(0.36, 0.56, uv.y));
    float branchB = (1.0 - smoothstep(0.004, 0.020, abs(uv.x + uv.y * 0.27 - 0.16)))
            * smoothstep(0.34, 0.48, uv.y) * (1.0 - smoothstep(0.48, 0.68, uv.y));

    return clamp((mainBolt + branchA * 0.55 + branchB * 0.42) * pulse, 0.0, 1.0);
}

void main() {
 vec3 rd=skyRay();
 vec3 col=overcastSky(rd);
 float bolt=lightningBolt(rd,uFlash);
 col+=vec3(0.76,0.84,0.92)*uFlash*smoothstep(0.02,0.82,rd.y)*(0.35+fbm(rd.xz*9.0)*0.45)*0.42;
 col=mix(col,vec3(0.76,0.84,0.92),clamp(bolt,0.0,0.86));
 fragColor=vec4(col*uBrightness,uAlpha);
}

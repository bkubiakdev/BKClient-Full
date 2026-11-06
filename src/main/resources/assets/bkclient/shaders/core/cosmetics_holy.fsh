#version 150

uniform vec2 ScreenSize;
uniform float GameTime;

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPosition;

out vec4 fragColor;

const int NUM_OCTAVES = 4;
const float ALPHA_CUTOUT = 0.1;
const float TICKS_PER_WRAP = 24000.0;
const float TICKS_PER_SECOND = 20.0;
const float TIME_WRAP_SECONDS = 100.0;
const float FIELD_SCALE = 8.0;
const float FIELD_DRIFT = 0.1;
const float GRADIENT_CURVE = 1.6;
const float OUTPUT_GAIN = 1.9;
const vec3 OUTPUT_FLOOR = vec3(0.10, 0.0, 0.09);
const vec3 LUMINANCE_WEIGHTS = vec3(0.299, 0.587, 0.114);
const float DETAIL_FLOOR = 0.25;
const float DETAIL_GAIN = 1.05;

const float MIN_NORMAL_LENGTH = 0.0001;
const float FRESNEL_F0 = 0.04;
const float FRESNEL_POWER = 5.0;
const float FRESNEL_FADE_START = 0.05;
const float FRESNEL_FADE_END = 0.7;
const float FRESNEL_STRENGTH = 0.5;
const float FRESNEL_TINT_MIX = 0.35;

const vec3 TONE_LOW = vec3(126.0 / 255.0, 0.0, 97.0 / 255.0);
const vec3 TONE_HIGH = vec3(173.0 / 255.0, 0.0, 161.4 / 255.0);
const vec3 SHADE_LOW = vec3(0.2, 0.0, 0.0);
const vec3 SHADE_HIGH = vec3(164.0 / 255.0, 1.0 / 255.0, 214.4 / 255.0);
const vec3 VEIN_LOW = vec3(0.1);
const vec3 VEIN_HIGH = vec3(0.9);

float holyRand(vec2 n) {
    return fract(cos(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float holyNoise(vec2 n) {
    const vec2 d = vec2(0.0, 1.0);
    vec2 b = floor(n);
    vec2 f = smoothstep(vec2(0.0), vec2(1.0), fract(n));
    return mix(mix(holyRand(b), holyRand(b + d.yx), f.x), mix(holyRand(b + d.xy), holyRand(b + d.yy), f.x), f.y);
}

float holyFbm(vec2 n) {
    float total = 0.0;
    float amplitude = 1.6;
    for (int i = 0; i < NUM_OCTAVES; i++) {
        total += holyNoise(n) * amplitude;
        n += n;
        amplitude *= 0.5;
    }
    return total;
}

void main() {
    vec4 base = texture(Sampler0, texCoord0);
    if (base.a < ALPHA_CUTOUT) {
        discard;
    }

    float time = mod(GameTime * TICKS_PER_WRAP / TICKS_PER_SECOND, TIME_WRAP_SECONDS);
    vec2 fragCoord = gl_FragCoord.xy;

    vec2 p = fragCoord * FIELD_SCALE / ScreenSize.xx;
    float q = holyFbm(p - time * FIELD_DRIFT);
    vec2 r = vec2(holyFbm(p + q + time - p.x - p.y), holyFbm(p + q - time));
    vec3 c = mix(TONE_LOW, TONE_HIGH, holyFbm(p + r)) + mix(SHADE_LOW, SHADE_HIGH, r.y) - mix(VEIN_LOW, VEIN_HIGH, r.x);

    float grad = fragCoord.y / ScreenSize.y;
    vec3 field = c * cos(GRADIENT_CURVE * grad) * (1.0 - grad);
    field = max(field, vec3(0.0)) * OUTPUT_GAIN + OUTPUT_FLOOR;

    float detail = dot(base.rgb, LUMINANCE_WEIGHTS);
    vec3 color = field * (DETAIL_FLOOR + detail * DETAIL_GAIN);

    vec3 normal = viewNormal / max(length(viewNormal), MIN_NORMAL_LENGTH);
    vec3 view = normalize(-viewPosition);
    float cosTheta = clamp(abs(dot(normal, view)), 0.0, 1.0);
    float fresnel = FRESNEL_F0 + (1.0 - FRESNEL_F0) * pow(1.0 - cosTheta, FRESNEL_POWER);
    float edge = smoothstep(FRESNEL_FADE_START, FRESNEL_FADE_END, fresnel);
    color += mix(vec3(1.0), field, FRESNEL_TINT_MIX) * edge * FRESNEL_STRENGTH;

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a * vertexColor.a);
}

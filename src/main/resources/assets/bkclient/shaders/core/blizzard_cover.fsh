#version 150

uniform vec3 CameraOrigin;
uniform vec3 TintColor;
uniform float CoverProgress;
uniform vec4 CoverFogColor;
uniform vec2 CoverFogRange;
in vec4 FragColor;
in vec3 WorldPos;
in float FogDistance;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1, 0)), u.x),
               mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0, amplitude = 0.5;
    mat2 rotation = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; ++i) {
        value += amplitude * noise(p);
        p = rotation * p * 2.0 + vec2(100.0);
        amplitude *= .5;
    }
    return value;
}

float hash13(vec3 p) {
    uvec3 v = uvec3(ivec3(floor(p)) + ivec3(1048576));
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    v ^= v >> 16;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    return float(v.x) * (1.0 / 4294967296.0);
}

void main() {
    vec3 normal = normalize(cross(dFdx(WorldPos), dFdy(WorldPos)));
    if (dot(normal, CameraOrigin - WorldPos) < 0.0) normal = -normal;
    vec3 axis = abs(normal);
    vec3 texel = floor((WorldPos - normal * .002) * 16.0);
    vec2 column = (texel.xz + .5) / 16.0;
    float field = fbm(column * .32);
    float powder = fbm(column * 5.5) - .5;
    float depth = clamp(CoverProgress, 0.0, 1.0) * 1.35 - (1.0 - field) * .95 + powder * .22;
    if (depth <= 0.0 || hash12(column * 16.0 + 17.31) > depth / .3) discard;
    // Stipple the range boundary instead of writing translucent quads into world depth.
    if (FragColor.a <= .001 || hash13(texel + 53.71) > FragColor.a) discard;
    float tone = hash13(texel + 3.97);
    vec3 albedo = tone < .6 ? vec3(1.0) : (tone < .88 ? vec3(.95, .98, .98) : vec3(.89, .94, .95));
    float shade = axis.y > .5 ? 1.0 : (axis.x > axis.z ? .6 : .8);
    vec3 color = TintColor * albedo * shade * FragColor.r;
    float fog = smoothstep(CoverFogRange.x, CoverFogRange.y, FogDistance) * CoverFogColor.a;
    fragColor = vec4(clamp(mix(color, CoverFogColor.rgb, fog), 0.0, 1.0), 1.0);
}

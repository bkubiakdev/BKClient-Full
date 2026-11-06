#version 150
uniform float uTime, uSpeed, uAlpha, uFov, uBrightness, uStarSize, uStarDensity;
uniform vec2 uResolution;
uniform vec3 uColor, uCamRight, uCamUp, uCamForward;
in vec2 vScreen;
out vec4 fragColor;
vec3 skyRay() {
 float f=tan(radians(uFov)*0.5);
 return normalize(uCamForward + uCamRight*vScreen.x*(uResolution.x/uResolution.y)*f + uCamUp*vScreen.y*f);
}

float hash13(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

vec3 starfield(vec3 d, float t, vec3 tint) {
    vec3 result = vec3(0.0);

    // Слой A — редкие крупные.
    {
        vec3 p = d * 80.0;
        vec3 i = floor(p);
        float h = hash13(i);
        if (h > 1.0 - 0.013 * uStarDensity) {
            vec3 f = fract(p) - 0.5;
            float dist2 = dot(f, f) / (uStarSize * uStarSize);
            float core = exp(-dist2 * 800.0);
            float halo = exp(-dist2 *  90.0) * 0.30;
            float bright = (h - (1.0 - 0.013 * uStarDensity)) / (0.013 * uStarDensity);
            float twinkle = 0.4 + 0.6 * sin(t * 2.0 + h * 113.0);
            // Лёгкий цветовой тинт по hash (тёплый/холодный белый).
            float tw = hash13(i + vec3(7.7));
            vec3 col = mix(vec3(0.92, 0.95, 1.00), vec3(1.00, 0.93, 0.85), tw);
            col = mix(col, tint, 0.10);
            result += col * (core + halo) * bright * twinkle;
        }
    }

    // Слой B — плотный мелкий.
    {
        vec3 p = d * 200.0;
        vec3 i = floor(p);
        float h = hash13(i + vec3(1.0));
        if (h > 1.0 - 0.009 * uStarDensity) {
            vec3 f = fract(p) - 0.5;
            float dist2 = dot(f, f) / (uStarSize * uStarSize);
            float core = exp(-dist2 * 1500.0);
            float bright = (h - (1.0 - 0.009 * uStarDensity)) / (0.009 * uStarDensity);
            float twinkle = 0.5 + 0.5 * sin(t * 1.5 + h * 91.0);
            result += vec3(0.95, 0.95, 1.00) * core * bright * twinkle * 0.7;
        }
    }

    return result;
}


void main() {
    vec3 d = skyRay();
    vec3 accent = max(uColor, vec3(0.05));
    vec3 sky = mix(vec3(0.003, 0.003, 0.012), accent * 0.04, 0.40);
    sky = mix(sky, accent * 0.12, 1.0 - smoothstep(0.0, 0.5, abs(d.y)));
    vec3 stars = starfield(d, uTime * uSpeed, accent);
    fragColor = vec4((sky + stars) * uBrightness, uAlpha);
}

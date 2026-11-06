#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D maskTexture;  // силуэт сущностей, полное разрешение
uniform sampler2D glowTexture;  // размытый силуэт (dual-Kawase), половина разрешения

uniform vec4 mainColor;   // rgb — основной цвет, a — общая прозрачность
uniform vec4 friendColor; // rgb — цвет друзей, a — 1 если подсветка друзей включена
uniform vec4 params1;     // texelX, texelY, outlineWidth, outlineMode (0 снаружи, 1 внутри, 2 обе)
uniform vec4 params2;     // glowStrength, glowFalloff, fillOpacity, innerGlow
uniform vec4 params3;     // outlineStrength, outlineWhite, glowEnabled, fillEnabled
uniform vec4 params4;     // shimmerT, shimmerEnabled, shimmerWidth, shimmerBrightness
uniform vec4 params5;     // pulseAmount, pulseT, saturation, outlineEnabled

float maxChan(vec3 c) {
    return max(c.r, max(c.g, c.b));
}

// Друзья рисуются в маску цветом 0x55FF55 (G/R ≈ 3.0), остальные — белым (G/R = 1.0).
// Сравнение каналов не зависит от яркости, поэтому работает и на слабом свечении.
bool isFriendTint(vec3 c) {
    return c.g > c.r * 1.5;
}

// Расстояние в пикселях до ближайшего пикселя противоположного состояния:
// снаружи — до силуэта, внутри — до фона. Возвращает большое число, если границы рядом нет.
float edgeDistance(int w, bool inside) {
    vec2 ts = params1.xy;
    float best = 1e6;
    for (int y = -w; y <= w; y++) {
        for (int x = -w; x <= w; x++) {
            vec2 off = vec2(float(x), float(y));
            float m = maxChan(textureLod(maskTexture, texCoord + off * ts, 0.0).rgb);
            bool other = inside ? (m < 0.5) : (m >= 0.5);
            if (other) best = min(best, length(off));
        }
    }
    return best;
}

void main() {
    vec3 maskS = texture(maskTexture, texCoord).rgb;
    vec3 glowS = texture(glowTexture, texCoord).rgb;

    float sharp = maxChan(maskS);
    float blur  = clamp(maxChan(glowS), 0.0, 1.0);
    bool inside = sharp >= 0.5;

    // всё, что дальше ореола — не наша забота
    if (!inside && blur < 0.0015) discard;

    vec3 tint = mainColor.rgb;
    if (friendColor.a > 0.5 && isFriendTint(inside ? maskS : glowS)) {
        tint = friendColor.rgb;
    }
    float grey = dot(tint, vec3(0.2126, 0.7152, 0.0722));
    tint = clamp(mix(vec3(grey), tint, params5.z), 0.0, 1.0);

    float pulse = 1.0 + params5.x * sin(params5.y * 6.2831853);

    // --- внешний ореол ---
    float halo = 0.0;
    if (params3.z > 0.5 && !inside) {
        halo = pow(blur, params2.y) * params2.x;
    }

    // --- заливка силуэта + внутреннее свечение к краям ---
    float fill = 0.0;
    if (params3.w > 0.5 && inside) {
        fill = params2.z + pow(clamp(1.0 - blur, 0.0, 1.0), 1.5) * params2.w;
    }

    // --- обводка по границе силуэта ---
    float rim = 0.0;
    int w = int(params1.z + 0.5);
    if (params5.w > 0.5 && w > 0) {
        int mode = int(params1.w + 0.5);
        if (mode == 2 || (mode == 0 && !inside) || (mode == 1 && inside)) {
            float d = edgeDistance(w, inside);
            if (d <= float(w)) {
                rim = (1.0 - d / (float(w) + 1.0)) * params3.x;
            }
        }
    }

    float body = (halo + fill) * pulse;
    vec3 rgb = tint * body;
    rgb += mix(tint, vec3(1.0), clamp(params3.y, 0.0, 1.0)) * rim * pulse;

    // --- бегущий блик по вертикали ---
    if (params4.y > 0.5) {
        float sweep = 1.0 - smoothstep(0.0, params4.z, abs(texCoord.y - params4.x));
        rgb += vec3(sweep * params4.w * max(body, rim));
    }

    rgb *= mainColor.a;

    if (maxChan(rgb) < 0.002) discard;

    // блендинг аддитивный (SRC_ALPHA, ONE) — яркость уже в rgb,
    // пересвет по каналам сам даёт белое ядро на кромке
    fragColor = vec4(rgb, 1.0);
}

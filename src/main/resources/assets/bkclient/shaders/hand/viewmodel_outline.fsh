#version 330 core

in vec2 uv;
out vec4 outColor;

uniform sampler2D ColorTexture;
uniform sampler2D DepthTexture;
uniform float uAlpha;
uniform float uSide;   // -1 = left hand only, +1 = right hand only, 0 = both
uniform vec3 uColor;
uniform vec2 resolution;

void main() {
    if (uSide != 0.0) {
        float halfSign = sign(uv.x - 0.5);
        if (halfSign == 0.0) halfSign = 1.0;
        if (uSide < 0.0 && uv.x >= 0.5) discard;
        if (uSide > 0.0 && uv.x <= 0.5) discard;
    }

    vec4 originalColor = texture(ColorTexture, uv);
    vec2 texelSize = 1.0 / textureSize(DepthTexture, 0);

    float centerDepth = texture(DepthTexture, uv).r;

    float edgeStrength = 0.0;
    float outwardGlow = 0.0;
    int radius = 6;

    for (int y = -radius; y <= radius; y++) {
        for (int x = -radius; x <= radius; x++) {
            if (x == 0 && y == 0) continue;
            vec2 offset = vec2(x, y) * texelSize;
            float neighborDepth = texture(DepthTexture, uv + offset).r;

            float dist = length(vec2(x, y));

            if (neighborDepth > 0.999 && centerDepth < 0.999) {
                edgeStrength += (1.0 / (dist + 0.1));
            }

            if (centerDepth > 0.999 && neighborDepth < 0.999) {
                float falloff = 1.0 - (dist / float(radius));
                falloff = max(falloff, 0.0);
                falloff = pow(falloff, 2.0);
                outwardGlow += falloff * 0.25;
            }
        }
    }

    edgeStrength = clamp(edgeStrength * 0.12, 0.0, 1.0);
    outwardGlow = clamp(outwardGlow, 0.0, 1.0);

    if (centerDepth > 0.999 && outwardGlow > 0.01) {
        outColor = vec4(uColor * outwardGlow, outwardGlow * uAlpha);
        return;
    }

    if (centerDepth < 0.999 && edgeStrength > 0.01) {
        outColor = vec4(uColor * edgeStrength, edgeStrength * uAlpha);
        return;
    }

    discard;
}

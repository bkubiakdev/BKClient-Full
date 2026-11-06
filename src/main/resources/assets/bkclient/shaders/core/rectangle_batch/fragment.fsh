#version 150

#moj_import <bkclient:common.glsl>

// Same math as core/rectangle/fragment.fsh; Size comes per rectangle and the corner radius is
// half the height (pill), which is what the ESP labels pass to the unbatched rectangle.
in vec2 FragCoord;
flat in vec2 RectSize;
in vec4 FragColor;

uniform float Smoothness;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 Size = RectSize;
    vec4 Radius = vec4(Size.y / 2.0);
    vec2 center = Size * 0.5;
    float alpha;
    if (Smoothness < 0.0) {
        float distance = roundedBoxSDF(center - (FragCoord * Size), center + Smoothness, Radius);
        float halfPixel = max(fwidth(distance) * 0.5, 0.0001);
        alpha = 1.0 - smoothstep(-halfPixel, halfPixel, distance);
    } else {
        float distance = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);
        alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);
    }
    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}

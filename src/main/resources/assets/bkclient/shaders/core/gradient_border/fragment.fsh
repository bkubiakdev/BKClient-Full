#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform float Time;
uniform float Alpha;
uniform float Thickness;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 uv = FragCoord;

    // Distance from nearest edge — 0 exactly at the screen border
    float dx = min(uv.x, 1.0 - uv.x);
    float dy = min(uv.y, 1.0 - uv.y);
    float edgeDist = min(dx, dy);

    // Border alpha — flush with edge, soft inner glow
    float border = smoothstep(Thickness, 0.0, edgeDist);

    // Soft bloom
    float bloom = exp(-edgeDist * 8.0 / Thickness) * 0.3;
    border = max(border, bloom);

    if (border < 0.001) {
        discard;
    }

    // Perimeter position 0..1 — atan handles corners seamlessly
    vec2 d = uv - 0.5;
    float perimeter = fract(atan(d.y, d.x) / 6.283185 + 0.25);

    // Animate along perimeter
    float flow = fract(perimeter + Time * 0.06);

    // Three colors: blue, pink, yellow
    vec3 blue   = vec3(0.30, 0.60, 1.00);
    vec3 pink   = vec3(1.00, 0.45, 0.70);
    vec3 yellow = vec3(1.00, 0.88, 0.30);

    // Smooth 3-stop gradient
    vec3 color;
    if (flow < 0.333) {
        color = mix(blue, pink, flow / 0.333);
    } else if (flow < 0.666) {
        color = mix(pink, yellow, (flow - 0.333) / 0.333);
    } else {
        color = mix(yellow, blue, (flow - 0.666) / 0.334);
    }

    // Soft brightness pulse
    float pulse = 0.92 + 0.08 * sin(Time * 1.2 + perimeter * 6.283);
    color *= pulse;

    // Edge glow
    float edgeGlow = 1.0 + 0.6 * exp(-edgeDist * 25.0 / Thickness);
    color *= edgeGlow;

    OutColor = vec4(color, border * Alpha * ColorModulator.a);
}

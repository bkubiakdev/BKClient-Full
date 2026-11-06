#version 150

in vec3 Position;
in vec2 UV;
in float Alpha;

uniform vec2 ScreenSize;

out vec2 texCoord;
out float vertexAlpha;

void main() {
    // Positions arrive in screen pixels with y growing downwards, the way the GUI
    // works, so flip y on the way into clip space.
    vec2 ndc = vec2(
        Position.x / ScreenSize.x * 2.0 - 1.0,
        1.0 - Position.y / ScreenSize.y * 2.0
    );
    gl_Position = vec4(ndc, 0.0, 1.0);
    texCoord = UV;
    vertexAlpha = Alpha;
}

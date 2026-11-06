#version 150

uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) discard;
    // These authored light strips keep their texture detail without scene lighting or fog.
    fragColor = color;
}

#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewPosition;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;

    viewPosition = view.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
}

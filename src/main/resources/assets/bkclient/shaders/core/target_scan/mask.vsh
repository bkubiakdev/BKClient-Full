#version 150
in vec3 Position;
in vec2 UV0;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 InverseView;
out vec2 texCoord;
out vec3 surfaceNormal;
void main() {
    texCoord = UV0;
    surfaceNormal = mat3(InverseView) * Normal;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}

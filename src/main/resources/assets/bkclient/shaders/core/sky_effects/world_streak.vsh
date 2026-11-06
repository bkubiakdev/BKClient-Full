#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 WorldView;
uniform mat4 WorldProjection;
out vec2 texCoord;
out vec4 vertexColor;
void main() {
    texCoord=UV0;
    vertexColor=Color;
    gl_Position=WorldProjection*WorldView*vec4(Position,1.0);
}

#version 330 core

layout(location = 0) in vec3 Position;

out vec2 vUV;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vUV = (Position.xy + 1.0) * 0.5;
}

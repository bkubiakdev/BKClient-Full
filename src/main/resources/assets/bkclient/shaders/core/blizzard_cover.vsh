#version 150

in vec3 Position;
in vec4 Color;
uniform mat4 WorldView;
uniform mat4 WorldProjection;
uniform vec3 CameraOrigin;
uniform float CoverFogShape;
out vec4 FragColor;
out vec3 WorldPos;
out float FogDistance;

void main() {
    gl_Position = WorldProjection * WorldView * vec4(Position, 1.0);
    FragColor = Color;
    WorldPos = Position + CameraOrigin;
    FogDistance = CoverFogShape > 0.5 ? max(length(Position.xz), abs(Position.y)) : length(Position);
}

#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 TexCoord;
out vec4 FragColor;
out vec2 GlobalPos;

void main() {
    TexCoord = UV0;
    FragColor = Color;
    
    vec4 clipPos = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position = clipPos;
    
    GlobalPos = clipPos.xy / clipPos.w * 0.5 + 0.5;
}

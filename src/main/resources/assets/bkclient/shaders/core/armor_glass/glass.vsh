#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out vec2 faceUV;
void main(){
    vec4 pos=ModelViewMat*vec4(Position,1.0);
    texCoord=UV0;faceUV=vec2(UV1);
    gl_Position=ProjMat*pos;
}

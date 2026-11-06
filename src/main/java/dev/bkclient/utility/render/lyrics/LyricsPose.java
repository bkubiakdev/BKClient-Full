package dev.bkclient.utility.render.lyrics;

import org.joml.Quaternionf;
import org.joml.Vector3d;

/** Mutates only the enabled component. Turning a toggle off freezes the last visible pose. */
public final class LyricsPose {
    public final Vector3d position;
    public final Quaternionf rotation;
    public LyricsPose(Vector3d position,Quaternionf rotation) { this.position=new Vector3d(position); this.rotation=new Quaternionf(rotation); }
    public void follow(Vector3d target,boolean enabled,double dt,float smoothingMs) {
        if(enabled)position.lerp(target,smoothingMs<=0||dt>.5?1:1-Math.exp(-Math.max(0,dt)*1000/smoothingMs));
    }
    public void face(Vector3d camera,boolean enabled) {
        if(!enabled)return;
        double x=camera.x-position.x,y=camera.y-position.y,z=camera.z-position.z;
        if(x*x+y*y+z*z>1e-10)rotation.rotationY((float)Math.atan2(x,z)).rotateX((float)-Math.atan2(y,Math.hypot(x,z)));
    }
}

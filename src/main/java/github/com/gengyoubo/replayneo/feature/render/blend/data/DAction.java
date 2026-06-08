package github.com.gengyoubo.replayneo.feature.render.blend.data;

import org.blender.dna.BezTriple;
import org.blender.dna.FCurve;
import org.blender.dna.bAction;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DAction {
    public final DId id = new DId(BlockCodes.ID_AC);
    public final List<DFCurve> curves = new ArrayList<>();

    public CPointer<bAction> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, bAction.class, () -> bAction -> serializer.writeDataList(FCurve.class, bAction.getCurves(), curves.size(), (i, fCurve) -> {
            try {
                DFCurve dfCurve = curves.get(i);
                fCurve.setRna_path(serializer.writeString0(dfCurve.rnaPath));
                fCurve.setArray_index(dfCurve.rnaArrayIndex);
                fCurve.setTotvert(dfCurve.keyframes.size());
                fCurve.setBezt(serializer.writeData(BezTriple.class, dfCurve.keyframes.size(), (j, fBezTriple) -> {
                    try {
                        DKeyframe dKeyframe = dfCurve.keyframes.get(j);
                        fBezTriple.setIpo((byte) dKeyframe.interpolationType.ordinal());
                        CArrayFacade<Float> vec = fBezTriple.getVec().get(1);
                        vec.set(0, (float) dKeyframe.frame);
                        vec.set(1, dKeyframe.value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static class DFCurve {
        public String rnaPath;
        public int rnaArrayIndex;
        public final List<DKeyframe> keyframes = new ArrayList<>();
    }

    public static class DKeyframe {
        public InterpolationType interpolationType = InterpolationType.CONSTANT;
        public int frame;
        public float value;
    }

    public enum InterpolationType {
        CONSTANT, LINEAR
    }
}

package version2.prototype;

import java.io.Serializable;

/**
 * Holds projection information.
 *
 * @author Michael DeVos
 * @author Isaiah Snell-Feikema
 */
public class Projection implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ResamplingType {
        NEAREST_NEIGHBOR,
        BILINEAR,
        CUBIC_CONVOLUTION
    }

    private ResamplingType resamplingType;
    private int pixelSize;

    /**
     *
     *
     * @param resamplingType
     * @param pixelSize
     */
    public Projection(
            ResamplingType resamplingType,
            int pixelSize) {

        this.resamplingType = resamplingType;
        this.pixelSize = pixelSize;
    }

    public Projection(Projection other) {
        resamplingType = other.resamplingType;
        pixelSize = other.pixelSize;
    }

    public ResamplingType getResamplingType() {
        return resamplingType;
    }

    public void setResamplingType(ResamplingType resamplingType) {
        this.resamplingType = resamplingType;
    }

    public int getPixelSize() {
        return pixelSize;
    }

    public void setPixelSize(int pixelSize) {
        this.pixelSize = pixelSize;
    }

    @Override
    public String toString() {
        return new StringBuilder()
        .append("{resampling type: ").append(resamplingType)
        .append(", pixel size: ").append(Integer.toString(pixelSize))
        .append("}").toString();
    }

}

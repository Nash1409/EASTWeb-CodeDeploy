package version2.prototype.util;

@SuppressWarnings("serial")
// Exception handling for ReadShapefile
public class ShapefileException extends Exception{
    public ShapefileException(){
        super();
    }

    public ShapefileException(String message){
        super(message);
    }
}
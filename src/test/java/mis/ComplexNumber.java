package mis;

/**
 * @author Chao Chen
 */
public class ComplexNumber {
    private final double re;
    private final double im;

    public ComplexNumber(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public String toString() {
        return "(" + re + " + " + im + "i)";
    }
    public static void main(String args[]){
        ComplexNumber compNo = new ComplexNumber(10, 15);
        System.out.println("Complex number = " + compNo);
    }
}

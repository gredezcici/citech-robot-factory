package jmh;

class FalseSharing {
    public static void main(String[] args) {
        String sm = "wngndkbsglmkbsfn;dhgndghmgm";
        sm.substring(0,10);

    }
}
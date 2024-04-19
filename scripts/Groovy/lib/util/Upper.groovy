package lib.util

class Upper {
    public static final Upper DEFAULT = new Upper()

    Object transform(String input) {
        return input ? input.toUpperCase() : input
    }
}
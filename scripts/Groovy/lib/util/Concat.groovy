package lib.util

class Concat {
    String left
    String right

    Object transform(String input) {
        StringBuilder builder = new StringBuilder()

        if (input) {
            if (left) {
                builder.append(left)
            }
            builder.append(input)

            if (right) {
                builder.append(right)
            }
        }

        return builder.toString()
    }
}
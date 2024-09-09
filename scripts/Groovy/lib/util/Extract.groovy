package lib.util

import java.util.logging.Logger
import java.util.regex.Pattern

class Extract {
    static final Logger LOGGER = Logger.getLogger('Extract.log')

    Pattern pattern

    Object transform(String input) {
        String output = null

        if (input) {
            def matcher = pattern.matcher(input)

            if (matcher.matches()) {
                output = matcher.group(1)
                LOGGER.fine(output)
            } else {
                LOGGER.fine('no match')
            }
        }

        return output
    }
}
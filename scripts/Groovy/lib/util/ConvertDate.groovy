package lib.util

import java.util.logging.Logger
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.temporal.UnsupportedTemporalTypeException

class ConvertDate {
    static final Logger LOGGER = Logger.getLogger('ConvertDate.log')

    DateTimeFormatter inputFormat
    DateTimeFormatter outputFormat

    Object transform(String input) {
        String outputDate = null

        if (input) {
            def inputDate = inputFormat.parse(input)

            try {
                outputDate = outputFormat.format(inputDate)
            } catch (UnsupportedTemporalTypeException exception) {
                LOGGER.fine({ "Exception when formatting ${input}: ${exception.message}".toString() })
                outputDate = outputFormat.format(LocalDateTime.from(inputDate).atZone(inputFormat.zone))
            }
        }

        return outputDate
    }
}
package lib.util

@Grab(group = 'net.sf.supercsv', module = 'super-csv', version = '2.4.0')

import org.supercsv.io.CsvMapWriter

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AsyncCsvWriter {
    CsvMapWriter csvWriter
    LinkedBlockingQueue queue = new LinkedBlockingQueue(1000)
    boolean running = false
    boolean stopped = true
    boolean headerNotWritten = true
    String[] outputHeaders

    AsyncCsvWriter(CsvMapWriter csvWriter) {
        this.csvWriter = csvWriter
        start()
    }

    void write(def record) {
        queue.put(record)
    }

    void start() {
        if (stopped) {
            running = true
            stopped = false

            Thread.start {
                List<Map> recordsToWrite = []

                while (running || queue.size() > 0) {
                    queue.drainTo(recordsToWrite)

                    // In case the queue is empty, add a wait time to reduce CPU usage
                    if (!recordsToWrite) {
                        def record = queue.poll(1000, TimeUnit.MILLISECONDS)
                        if (record) {
                            recordsToWrite.add(record)
                        }
                    }

                    recordsToWrite.each { record ->
                        if (headerNotWritten) {
                            outputHeaders = record.keySet().toArray(new String[1])
                            csvWriter.writeHeader(outputHeaders)
                            headerNotWritten = false
                        }

                        csvWriter.write(record, outputHeaders)
                    }
                    recordsToWrite.clear()
                }
                stopped = true
            }
        }
    }

    void stop() {
        running = false

        if (!stopped) {
            Thread.sleep(100)
        }
    }
}
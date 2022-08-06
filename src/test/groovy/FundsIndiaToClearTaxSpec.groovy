import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

/**
 * Test class for {@code FundsIndiaToClearTax} script
 */
class FundsIndiaToClearTaxSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    GroovyShell shell = new GroovyShell(new Binding())

    void 'test conversion'() {
        given:
        File inputFile = new File(FundsIndiaToClearTaxSpec.class.classLoader.getResource('input-fundsindia.csv').toURI())
        File outputFile = temporaryFolder.newFile('output.csv')
        String expectedOutput = FundsIndiaToClearTaxSpec.class.classLoader.getResource('output-cleartax.csv').text

        when:
        shell.run(new File('scripts/Groovy/FundsIndiaToClearTax.groovy'),
                '-i', inputFile.getAbsolutePath(),
                '-o', outputFile.getAbsolutePath())

        then:
        outputFile.text == expectedOutput
    }
}

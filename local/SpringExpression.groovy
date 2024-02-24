@Grab(group='org.springframework', module='spring-expression', version='5.3.22')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.13.3')

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.TypeConverter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

ExpressionParser parser = new SpelExpressionParser();
ParserContext templateParserContext = new TemplateParserContext('${', '}');
String templatedString = "flow.config: \${flow['config']}";
Dummy contextRoot = new Dummy();

GenericConversionService conversionService = new DefaultConversionService();
conversionService.addConverter(new ConverterLinkedHashMapToString());
TypeConverter typeConverter = new StandardTypeConverter(conversionService);
StandardEvaluationContext evalContext = new StandardEvaluationContext();
evalContext.setTypeConverter(typeConverter);

Expression exp = parser.parseExpression(templatedString, templateParserContext);
String message = (String) exp.getValue(evalContext, contextRoot);
System.out.println(message);

class Dummy {
    Map<String, ?> flow = ['config': ['hola': 'hello']];
}

class ConverterLinkedHashMapToString implements Converter<LinkedHashMap<?, ?>, String> {
    private ObjectMapper objMapper = new ObjectMapper();

    @Override
    public String convert(LinkedHashMap<?, ?> source) {
        return objMapper.writeValueAsString(source);
    }
} 
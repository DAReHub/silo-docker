package de.tum.bgu.msm.models.javascript;

import de.tum.bgu.msm.data.HouseholdType;
import de.tum.bgu.msm.models.autoOwnership.munich.MunichCarOwnershipJSCalculator;
import de.tum.bgu.msm.models.relocation.DwellingUtilityJSCalculator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by matthewokrah on 29/09/2017.
 */
public class DwellingUtilityCalculatorTest {

    private DwellingUtilityJSCalculator calculator;

    @Before
    public void setup() {
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("DwellingUtilityCalc"));
        calculator = new DwellingUtilityJSCalculator(reader);
    }

    @Test
    public void testModel() throws ScriptException {
        double expected = 0.82063;
        double result = calculator.calculateSelectDwellingUtility(HouseholdType.size1inc1, 0.9,0.9,0.9,0.9,0.9);
        result = calculator.personalizeUtility(HouseholdType.size1inc1, result, 0.45, 1);


            Assert.assertEquals(expected, result, 0.0001);


    }
}
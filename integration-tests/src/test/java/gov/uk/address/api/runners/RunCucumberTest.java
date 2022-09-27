package gov.uk.address.api.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {"json:target/cucumber.json", "html:target/default-html-reports"},
        features = "src/test/resources",
        glue = "gov/uk/address/api/stepDefinitions",
        dryRun = false)
public class RunCucumberTest {}

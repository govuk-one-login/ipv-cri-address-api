package gov.uk.address.api.stepDefinitions;

import io.cucumber.java.en.Given;

public class AddressApiHappyPath {
    @Given("user has the user identity in the form of a signed JWT string")
    public void user_has_the_user_identity_in_the_form_of_a_signed_jwt_string() {
        System.out.println("This is our Given");
    }

}

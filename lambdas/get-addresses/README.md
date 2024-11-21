# Get Addresses

Get addresses takes a session Id and returns any addresses stored against that Id.

## How to run tests

To run all tests, run `npm run test`. This will compile and run all tests in the `/tests` directory.

### How to run an individual test file

To run an individual test file, you must pass the file name to the test command. For example `npm run test -- -t app.test.ts`. This will only execute the test file in question.

### How to run an individual test

To run an individual test, you must pass the file name to test command but also modify the test with the `only` attribute.

For example:
``` Javascript
it.only("will only run this test in this file",() => );
```
Note that if you dont specify Jest to run just the file with the test, then it will also run the other files in parallel.

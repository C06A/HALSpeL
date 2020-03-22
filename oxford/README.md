# HALSpeL. Sample of HAL layer API

This project contains a few sample functoins demonstrating how to use HALSpel to access HAL resources.

[![license](https://img.shields.io/github/license/C06A/HALSpeL.svg)](https://github.com/C06A/HALSpeL/blob/master/LICENSE)
[![Download Latest](https://img.shields.io/badge/download-1.5.2-green.svg)](https://github.com/C06A/HALSpeL/releases/download/v1.5.2/hal-spel-1.5.2.jar)

It accesses free HAL API services to get information about classes in Oxford and includes implementations in
[Kotlin](https://github.com/C06A/HALSpeL/blob/develop/oxford/src/main/kotlin/oxford/Oxford.kt),
[Groovy](https://github.com/C06A/HALSpeL/blob/develop/oxford/src/main/groovy/oxford/OxfordG.groovy)
and
[Java](https://github.com/C06A/HALSpeL/blob/develop/oxford/src/main/java/oxford/OxfordJ.java).

Also this module demonstrates how to use aspects to print information about requests and responses in format,
which AsciiDoctor uses to insert pieces into final document.
By redirecting output into file this can be used to build documentation by inserting these pieces into
template document where needed.

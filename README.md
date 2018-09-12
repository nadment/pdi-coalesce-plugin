# PDI Coalesce Plugin


## Overview

This plugin provide PDI step to return the first non-null value from a set of fields.

![Image](https://github.com/nadment/pdi-coalesce-plugin/blob/master/src/main/resources/coalesce.svg)


## How to install #

#### System Requirements

Pentaho Data Integration 8.0 or above

#### Using Pentaho Marketplace

1. Find the plugin in the [Pentaho Marketplace](http://www.pentaho.com/marketplace) and click Install
2. Restart Spoon

#### Manual Install

1. Place the “pdi-coalesce-plugin” folder in the ${DI\_HOME}/plugins/ directory
2. Restart Spoon


## Documentation

The order of the input fields listed in the columns determines the order in which they are evaluated.

The step can consider empty string as null and can remove input fields from stream

Support MetaData Injection (MDI) 


## Support

This plugin for PDI is provided “as is”, without any warranties, expressed or implied. This software is not covered by any Support Agreement.


## License

Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.



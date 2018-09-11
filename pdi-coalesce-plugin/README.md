# Overview #

This plugin provide PDI step to return the first non-null value from a set of fields.

# System Requirements #

Pentaho Data Integration 8.0 or above

# How to install #

## Using Pentaho Marketplace ##

1. In the [Pentaho Marketplace] (http://www.pentaho.com/marketplace) find the AS400 plugin and click Install
2. Restart Spoon

## Manual Install ##

1. Place the pdi-coalesce folder in the ${DI\_HOME}/plugins/ directory
2. Restart Spoon

# Usage #

The order of the input fields listed in the columns determines the order in which they are evaluated.

The step can consider empty string as null and can remove input fields from stream

Support MetaData Injection (MDI) 

# License #

Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.



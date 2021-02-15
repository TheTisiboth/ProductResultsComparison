package org.openlca.display;

import java.io.File;

import com.greendelta.cli.Argument;

class Input {

	@Argument(name = 'd', longName = "data", description = "Location of the database to calculate", required = true)
	File dbFile;
	@Argument(name = 'b', longName = "background", description = "Location of the background database to import before calculation")
	File backgroundDbFile;
	@Argument(name = 'm', longName = "methods", description = "Location of the method database to import and use during calculation")
	File methodDbFile;
	@Argument(name = 'o', longName = "output", description = "Location to write output to")
	File outputDir;
	@Argument(name = 'l', longName = "log", description = "Location to write the logs to")
	File logFile;
	@Argument(name = 'i', longName = "ignore", description = "Location of a text file containing ref ids to ignore/skip")
	File ignoreFile;
	@Argument(name = 'p', longName = "post", description = "Type of post processor")
	private String postProcessor;
	@Argument(name = 'u', longName = "unit", description = "Post process unit processes")
	boolean processUnit = false;
	@Argument(name = 's', longName = "system", description = "Post process lci results")
	boolean processSystem = false;
	@Argument(name = 'r', longName = "result", description = "Post process lcia results")
	boolean processResult = false;
	@Argument(name = 'w', longName = "overwrite", description = "Overwrite data")
	boolean overwrite = false;


}
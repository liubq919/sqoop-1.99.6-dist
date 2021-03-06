/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.shell;

import jline.ConsoleReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.sqoop.common.Direction;
import org.apache.sqoop.model.MJob;
import org.apache.sqoop.shell.core.Constants;
import org.apache.sqoop.shell.utils.ConfigDisplayer;
import org.apache.sqoop.shell.utils.ConfigOptions;
import org.apache.sqoop.shell.utils.JobDynamicConfigOptions;
import org.apache.sqoop.validation.Status;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import static org.apache.sqoop.shell.ShellEnvironment.*;
import static org.apache.sqoop.shell.utils.ConfigFiller.*;

/**
 * Handles creation of new job objects.
 */
@SuppressWarnings("serial")
public class CreateJobFunction extends  SqoopFunction {
  @SuppressWarnings("static-access")
  public CreateJobFunction() {
    this.addOption(OptionBuilder
      .withDescription(resourceString(Constants.RES_PROMPT_LINK_ID))
      .withLongOpt(Constants.OPT_FROM)
      .isRequired()
      .hasArg()
      .create(Constants.OPT_FROM_CHAR)
    );
    this.addOption(OptionBuilder
      .withDescription(resourceString(Constants.RES_PROMPT_LINK_ID))
      .withLongOpt(Constants.OPT_TO)
      .isRequired()
      .hasArg()
      .create(Constants.OPT_TO_CHAR)
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object executeFunction(CommandLine line, boolean isInteractive) throws IOException {
    return createJob(getLong(line, Constants.OPT_FROM),
                     getLong(line, Constants.OPT_TO),
                     line.getArgList(),
                     isInteractive);
  }

  private Status createJob(Long fromLinkId, Long toLinkId, List<String> args, boolean isInteractive) throws IOException {
    printlnResource(Constants.RES_CREATE_CREATING_JOB, fromLinkId, toLinkId);

    ConsoleReader reader = new ConsoleReader();
    MJob job = client.createJob(fromLinkId, toLinkId);

    ResourceBundle fromConfigBundle = client.getConnectorConfigBundle(
        job.getFromConnectorId());
    ResourceBundle toConfigBundle = client.getConnectorConfigBundle(
        job.getToConnectorId());
    ResourceBundle driverConfigBundle = client.getDriverConfigBundle();

    Status status = Status.OK;

    if (isInteractive) {
      printlnResource(Constants.RES_PROMPT_FILL_JOB_CONFIG);

      do {
        // Print error introduction if needed
        if( !status.canProceed() ) {
          errorIntroduction();
        }

        // Fill in data from user
        if(!fillJobWithBundle(reader, job, fromConfigBundle, toConfigBundle, driverConfigBundle)) {
          return null;
        }

        // Try to create
        status = client.saveJob(job);
      } while(!status.canProceed());
    } else {
      JobDynamicConfigOptions options = new JobDynamicConfigOptions();
      options.prepareOptions(job);
      CommandLine line = ConfigOptions.parseOptions(options, 0, args, false);
      if (fillJob(line, job)) {
        status = client.saveJob(job);
        if (!status.canProceed()) {
          printJobValidationMessages(job);
          return null;
        }
      } else {
        printJobValidationMessages(job);
        return null;
      }
    }

    ConfigDisplayer.displayConfigWarning(job);
    printlnResource(Constants.RES_CREATE_JOB_SUCCESSFUL, status.name(), job.getPersistenceId());

    return status;
  }
}

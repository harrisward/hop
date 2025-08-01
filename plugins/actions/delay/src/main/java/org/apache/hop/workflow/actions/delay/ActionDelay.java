/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.workflow.actions.delay;

import java.util.List;
import org.apache.hop.core.Const;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.Result;
import org.apache.hop.core.annotations.Action;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.action.ActionBase;
import org.apache.hop.workflow.action.validator.ActionValidatorUtils;
import org.apache.hop.workflow.action.validator.AndValidator;

/** Action type to sleep for a time. It uses a piece of javascript to do this. \ */
@Action(
    id = "DELAY",
    name = "i18n::ActionDelay.Name",
    description = "i18n::ActionDelay.Description",
    image = "Delay.svg",
    categoryDescription = "i18n:org.apache.hop.workflow:ActionCategory.Category.Conditions",
    keywords = "i18n::ActionDelay.keyword",
    documentationUrl = "/workflow/actions/waitfor.html")
public class ActionDelay extends ActionBase implements Cloneable {
  private static final Class<?> PKG = ActionDelay.class;

  private static final String DEFAULT_MAXIMUM_TIMEOUT = "0";

  // Maximum timeout in seconds
  @HopMetadataProperty(key = "maximumTimeout")
  private String maximumTimeout;

  @HopMetadataProperty(key = "scaletime")
  public int scaleTime;

  public ActionDelay(String n) {
    super(n, "");
  }

  public ActionDelay() {
    this("");
  }

  public ActionDelay(ActionDelay other) {
    super(other.getName(), other.getDescription(), other.getPluginId());
    this.maximumTimeout = other.maximumTimeout;
    this.scaleTime = other.scaleTime;
  }

  @Override
  public Object clone() {
    return new ActionDelay(this);
  }

  /**
   * Execute this action and return the result. In this case it means, just set the result boolean
   * in the Result class.
   *
   * @param previousResult The result of the previous execution
   * @return The Result of the execution.
   */
  @Override
  public Result execute(Result previousResult, int nr) {
    Result result = previousResult;
    result.setResult(false);
    int multiple;
    String waitscale;

    // Scale time
    switch (scaleTime) {
      case 0:
        // Second
        multiple = 1000;
        waitscale = BaseMessages.getString(PKG, "ActionDelay.SScaleTime.Label");
        break;
      case 1:
        // Minute
        multiple = 60000;
        waitscale = BaseMessages.getString(PKG, "ActionDelay.MnScaleTime.Label");
        break;
      default:
        // Hour
        multiple = 3600000;
        waitscale = BaseMessages.getString(PKG, "ActionDelay.HrScaleTime.Label");
        break;
    }

    try {
      // starttime (in seconds ,Minutes or Hours)
      double timeStart = (double) System.currentTimeMillis() / (double) multiple;

      double iMaximumTimeout =
          Const.toInt(getRealMaximumTimeout(), Const.toInt(DEFAULT_MAXIMUM_TIMEOUT, 0));

      if (isDetailed()) {
        logDetailed(
            BaseMessages.getString(
                PKG, "ActionDelay.LetsWaitFor.Label", iMaximumTimeout, waitscale));
      }

      boolean continueLoop = true;
      //
      // Sanity check on some values, and complain on insanity
      //
      if (iMaximumTimeout < 0) {
        iMaximumTimeout = Const.toInt(DEFAULT_MAXIMUM_TIMEOUT, 0);
        logBasic(
            BaseMessages.getString(
                PKG,
                "ActionDelay.MaximumTimeReset.Label",
                String.valueOf(iMaximumTimeout),
                String.valueOf(waitscale)));
      }

      // Loop until the delay time has expired.
      //
      while (continueLoop && !parentWorkflow.isStopped()) {
        // Update Time value
        double now = (double) System.currentTimeMillis() / (double) multiple;

        // Let's check the limit time
        if ((iMaximumTimeout >= 0) && (now >= (timeStart + iMaximumTimeout))) {
          // We have reached the time limit
          if (isDetailed()) {
            logDetailed(BaseMessages.getString(PKG, "ActionDelay.WaitTimeIsElapsed.Label"));
          }
          continueLoop = false;
          result.setResult(true);
        } else {
          Thread.sleep(100);
        }
      }
    } catch (Exception e) {
      // We get an exception
      result.setResult(false);
      logError("Error  : " + e.getMessage());

      if (Thread.currentThread().isInterrupted()) {
        Thread.currentThread().interrupt();
      }
    }

    return result;
  }

  @Override
  public boolean resetErrorsBeforeExecution() {
    // we should be able to evaluate the errors in
    // the previous action.
    return false;
  }

  @Override
  public boolean isEvaluation() {
    return true;
  }

  @Override
  public boolean isUnconditional() {
    return false;
  }

  public String getMaximumTimeout() {
    return maximumTimeout;
  }

  public String getRealMaximumTimeout() {
    return Const.trim(resolve(getMaximumTimeout()));
  }

  public void setMaximumTimeout(String s) {
    maximumTimeout = s;
  }

  @Override
  public void check(
      List<ICheckResult> remarks,
      WorkflowMeta workflowMeta,
      IVariables variables,
      IHopMetadataProvider metadataProvider) {
    ActionValidatorUtils.andValidator()
        .validate(
            this,
            "maximumTimeout",
            remarks,
            AndValidator.putValidators(ActionValidatorUtils.longValidator()));
    ActionValidatorUtils.andValidator()
        .validate(
            this,
            "scaleTime",
            remarks,
            AndValidator.putValidators(ActionValidatorUtils.integerValidator()));
  }

  public int getScaleTime() {
    return scaleTime;
  }

  public void setScaleTime(int scaleTime) {
    this.scaleTime = scaleTime;
  }
}

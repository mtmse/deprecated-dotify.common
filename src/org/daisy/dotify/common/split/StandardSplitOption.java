package org.daisy.dotify.common.split;

/**
 * Defines standard split options.
 * 
 * @author Joel Håkansson
 * @deprecated use the corresponding class in org.daisy.dotify.common.splitter
 */
@Deprecated
public enum StandardSplitOption implements SplitOption {

	/**
	 * Allow force to be used.
	 */
	ALLOW_FORCE,
	/**
	 * Retain trailing skippable units
	 */
	RETAIN_TRAILING;
}

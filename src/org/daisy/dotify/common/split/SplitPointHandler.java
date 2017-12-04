package org.daisy.dotify.common.split;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.daisy.dotify.common.collection.SplitList;
import org.daisy.dotify.common.split.SplitPointSpecification.Type;


/**
 * Breaks units into results. All allowed break points are supplied with the input.
 * 
 * @author Joel Håkansson
 *
 * @param <T> the type of split point units
 */
/**
 * @author Joel Håkansson
 *
 * @param <T> the type of object
 */
public class SplitPointHandler<T extends SplitPointUnit> {
	private final List<T> EMPTY_LIST = Collections.emptyList();
	private final SplitPointCost<T> defaultCost = new SplitPointCost<T>() {
		@Override
		public double getCost(SplitPointDataSource<T> data, int index, int breakpoint) {
			// 1. the smaller the result, the higher the cost
			// 2. breakable units are always preferred over forced ones
			return (data.get(index).isBreakable()?1:2)*breakpoint-index;
		}
	};
	
	/**
	 * Splits the data at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * @param breakPoint the split point
	 * @param units the data
	 * @return returns a split point result
	 */
	@SafeVarargs
	public final SplitPoint<T> split(float breakPoint, T ... units) {
		return split(breakPoint, new SplitPointDataList<>(units), defaultCost);
	}

	/**
	 * Splits the data at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * @param breakPoint the split point
	 * @param units the data
	 * @param options the split options
	 * @return returns a split point result
	 */
	public SplitPoint<T> split(float breakPoint, List<T> units, SplitOption ... options) {
		return split(breakPoint, new SplitPointDataList<T>(units), defaultCost, options);
	}
	
	/**
	 * Splits the data at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * @param breakPoint the split point
	 * @param units the data
	 * @param cost the cost function used when determining the optimal <i>forced</i> split point. In other words,
	 * 		 the cost function is only used if there are no breakable units available.
	 * @param options the split options
	 * @return returns a split point result
	 */
	public SplitPoint<T> split(float breakPoint, List<T> units, SplitPointCost<T> cost, SplitOption ... options) {
		return split(breakPoint, new SplitPointDataList<>(units), cost, options);
	}

	/**
	 * Splits the data at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * 
	 * @param breakPoint the split point
	 * @param data the data to split
	 * @param options the split options
	 * @return returns a split point result
	 */
	public SplitPoint<T> split(float breakPoint, SplitPointDataSource<T> data, SplitOption ... options) {
		return split(breakPoint, data, defaultCost, options);
	}

	/**
	 * Splits the data at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * 
	 * @param breakPoint the split point
	 * @param data the data to split
	 * @param cost the cost function used when determining the optimal <i>forced</i> split point. In other words,
	 * 		 the cost function is only used if there are no breakable units available.
	 * @param options the split options
	 * @return returns a split point result
	 * @throws IllegalArgumentException if cost is null
	 */
	public SplitPoint<T> split(float breakPoint, SplitPointDataSource<T> data, SplitPointCost<T> cost, SplitOption ... options) {
		SplitPointSpecification spec = find(breakPoint, data, cost, options);
		if (cost==null) {
			throw new IllegalArgumentException("Null cost not allowed.");
		}
		if (spec.getType()==Type.EMPTY) {
			// pretty simple...
			return new SplitPoint<>(EMPTY_LIST, EMPTY_LIST, SplitPointDataList.emptyManager(), EMPTY_LIST, false);
		} else if (spec.getType()==Type.NONE) {
			return emptyHead(data);
		} else if (spec.getType()==Type.ALL) {
			return finalizeBreakpoint(new SplitList<>(data.getRemaining(), EMPTY_LIST), SplitPointDataList.emptyManager(), data.getSupplements(), false);
		} else {
			return makeBreakpoint(data, spec);
		}
	}
	
	/**
	 * Splits the data according to the supplied specification. A specification can be created by using 
	 * {@link #find(float, SplitPointDataSource, SplitPointCost, SplitOption...)} on the data source.
	 * No data is beyond the specified split point is produced using this method.
	 * 
	 * @param spec the specification
	 * @param data the data
	 * @return returns a split point result
	 */
	public SplitPoint<T> split(SplitPointSpecification spec, SplitPointDataSource<T> data) {
		if (spec.getType()==Type.EMPTY) {
			// pretty simple...
			return new SplitPoint<>(EMPTY_LIST, EMPTY_LIST, SplitPointDataList.emptyManager(), EMPTY_LIST, false);
		} else if (spec.getType()==Type.NONE) {
			return emptyHead(data);
		} else if (spec.getType()==Type.ALL) {
			return finalizeBreakpoint(new SplitList<>(data.getRemaining(), EMPTY_LIST), SplitPointDataList.emptyManager(), data.getSupplements(), false);
		} else {
			return makeBreakpoint(data, spec);
		}
	}
	
	public SplitPointSpecification find(float breakPoint, SplitPointDataSource<T> data, SplitOption ... options) {
		return find(breakPoint, data, defaultCost, options);
	}

	/**
	 * Finds a split point at, or before, the supplied breakPoint according to the rules
	 * in the data. If force is used, rules may be broken to achieve a result.
	 * 
	 * @param breakPoint the split point
	 * @param data the data to split
	 * @param cost the cost function used when determining the optimal <i>forced</i> split point. In other words,
	 * 		 the cost function is only used if there are no breakable units available.
	 * @param options the split options
	 * @return returns a split point specification
	 */
	public SplitPointSpecification find(float breakPoint, SplitPointDataSource<T> data, SplitPointCost<T> cost, SplitOption ... options) {
		SplitOptions opts = SplitOptions.parse(options);
		if (cost==null) {
			throw new IllegalArgumentException("Null cost not allowed.");
		}
		if (data.isEmpty()) {
			// pretty simple...
			return SplitPointSpecification.empty();
		} else if (breakPoint<=0) {
			return SplitPointSpecification.none();
		} else if (fits(data, breakPoint)) {
			return SplitPointSpecification.all();
		} else {
			int startPos = findCollapse(data, new SizeStep<>(breakPoint, data.getSupplements()));
			// If no units are returned here it's because even the first unit doesn't fit.
			// Therefore, force will not help.
			if (startPos<0) {
				return SplitPointSpecification.none();
			} else {
				return findBreakpoint(data, opts.useForce, startPos, cost, opts.trimTrailing);
			}
		}		
	}
	
	private static class SplitOptions {
		boolean useForce = false;
		boolean trimTrailing = true;
		static SplitOptions parse(SplitOption ... opts) {
			SplitOptions result = new SplitOptions();
			for (SplitOption option : opts) {
				if (option==StandardSplitOption.ALLOW_FORCE) {
					result.useForce = true;
				} else if (option==StandardSplitOption.RETAIN_TRAILING) {
					result.trimTrailing = false;
				} else if (option == null) {
                   //no-op
				} else {
					throw new UnsupportedOperationException("'" + option +
                    "' is not a recognized split option");
				}
			}
			return result;
		}
	}
	
	private SplitPoint<T> emptyHead(SplitPointDataSource<T> data) {
		return finalizeBreakpoint(new SplitList<>(EMPTY_LIST, EMPTY_LIST), data, data.getSupplements(), false);
	}
	
	private SplitPointSpecification findBreakpoint(SplitPointDataSource<T> data, boolean force, int startPos, SplitPointCost<T> cost, boolean trimTrailing) {
		Supplements<T> map = data.getSupplements();
		int strPos = forwardSkippable(data, startPos);
		// check next unit to see if it can be removed.
		if (!data.hasElementAt(strPos+1)) { // last unit?
			return SplitPointSpecification.all();
		} else {
			return findBreakpointFromPosition(data, strPos, map, force, cost, trimTrailing);
		}
	}

	private SplitPoint<T> makeBreakpoint(SplitPointDataSource<T> data, SplitPointSpecification spec) {
		Supplements<T> map = data.getSupplements();
		SplitResult<T> split = getResult(data, spec.getIndex());
		return finalizeBreakpointFull(split, map, spec.isHard(), spec.shouldTrimTrailing());
	}
	
	private SplitPointSpecification findBreakpointFromPosition(SplitPointDataSource<T> data, int strPos, Supplements<T> map, boolean force, SplitPointCost<T> cost, boolean trimTrailing) {
		// back up
		BreakPointScannerResult result=findBreakpointBefore(data, strPos, cost);
		boolean hard = false;
		int tailStart;
		if (result.bestBreakable!=result.bestSplitPoint) { // no breakable found, break hard 
			if (force) {
				hard = true;
				tailStart = result.bestSplitPoint+1;
			} else {
				tailStart = 0;
			}
		} else {
			tailStart = result.bestBreakable+1;
		}
		return new SplitPointSpecification(tailStart, hard, trimTrailing);
	}

	private SplitPoint<T> finalizeBreakpointFull(SplitResult<T> result, Supplements<T> map, boolean hard, boolean trimTrailing) {
		if (trimTrailing) {
			return finalizeBreakpoint(trimTrailing(result.head()), result.tail(), map, hard);
		} else {
			return finalizeBreakpoint(new SplitList<>(result.head(), EMPTY_LIST), result.tail(), map, hard);
		}
	}

	private SplitPoint<T> finalizeBreakpoint(SplitList<T> head, SplitPointDataSource<T> tail, Supplements<T> map, boolean hard) {
		TrimStep<T> trimmed = new TrimStep<>(map);
		findCollapse(new SplitPointDataList<T>(head.getFirstPart()), trimmed);
		List<T> discarded = trimmed.getDiscarded();
		discarded.addAll(head.getSecondPart());
		return new SplitPoint<>(trimmed.getResult(), trimmed.getSupplements(), tail, discarded, hard);
	}

	/**
	 * Trims leading skippable units in the supplied list. The result is backed by the
	 * original list. 
	 * 
	 * @param in the list to trim
	 * @param <T> the type of split list
	 * @return the list split in two parts, one with the leading skippable units, one with
	 * the remainder
	 */
	public static <T extends SplitPointUnit> SplitList<T> trimLeading(List<T> in) {
		int i;
		for (i = 0; i<in.size(); i++) {
			if (!in.get(i).isSkippable()) {
				break;
			}
		}
		return SplitList.split(in, i);
	}

	/**
	 * Trims leading skippable units in the supplied data source. The result is backed by the
	 * original data source.
	 * 
	 * @param in the list to trim
	 * @param <T> the type of split list
	 * @return a split point, the leading skippable units are placed in {@link SplitPoint#getDiscarded()}, the
	 * remainder are placed in {@link SplitPoint#getTail()}
	 */
	public static <T extends SplitPointUnit> SplitPoint<T> trimLeading(SplitPointDataSource<T> in) {
		return skipLeading(in, findLeading(in));
	}
	
	/**
	 * Skips leading units in the supplied list. The result is backed by the original data source.
	 * No data is beyond index is produced using this method.  
	 * @param in the list to trim
	 * @param index the index of the split point
	 * @param <T> the type of object
	 * @return a split point, the leading units are placed in {@link SplitPoint#getDiscarded()}, the
	 * remainder are placed in {@link SplitPoint#getTail()}
	 */
	public static <T extends SplitPointUnit> SplitPoint<T> skipLeading(SplitPointDataSource<T> in, int index) {
		SplitResult<T> res = in.split(index);
		return new SplitPoint<T>(null, null, res.tail(), res.head(), false);
	}
	
	/**
	 * Finds leading skippable units in the supplied data source.
	 * @param in the data source to search
	 * @param <T> the type of object
	 * @return returns the index of the first non-skippable unit
	 */
	public static <T extends SplitPointUnit> int findLeading(SplitPointDataSource<T> in) {
		int i;
		for (i = 0; in.hasElementAt(i); i++) {
			if (!in.get(i).isSkippable()) {
				break;
			}
		};
		return i;
	}

	static <T extends SplitPointUnit> T maxSize(T u1, T u2) {
		return (u1.getUnitSize()>=u2.getUnitSize()?u1:u2); 
	}
	
	static <T extends SplitPointUnit> SplitList<T> trimTrailing(List<T> in) {
		int i;
		for (i = in.size()-1; i>=0; i--) {
			if (!in.get(i).isSkippable()) {
				break;
			}
		}
		return SplitList.split(in, i+1);
	}
	
	static <T extends SplitPointUnit> SplitResult<T> getResult(SplitPointDataSource<T> data, int tailStart) {
		if (tailStart==0) {
			return new SplitResult<T>(Collections.emptyList(), data);
		} else if (data.hasElementAt(tailStart-1)) {
			return data.split(tailStart);
		} else {
			return new SplitResult<T>(data.getRemaining(), SplitPointDataList.emptyManager());
		}
	}
	
	/**
	 * Finds the index for the last unit that fits into the given space
	 * @param data
	 * @param impl
	 * @return returns the index for the last unit
	 */
	static <T extends SplitPointUnit> int findCollapse(SplitPointDataSource<T> data, StepForward<T> impl) {
		int units = -1;
		T maxCollapsable = null;
		for (int i=0; data.hasElementAt(i); i++) {
			T c = data.get(i);
			units++;
			if (c.isCollapsible()) {
				if (maxCollapsable!=null) {
					if (maxCollapsable.collapsesWith(c)) {
						if (maxSize(maxCollapsable, c)==c) {
							//new one is now max, add the previous to collapsed
							impl.addDiscarded(maxCollapsable);
							maxCollapsable = c;
						} else {
							//old one is max, add the new one to collapsed
							impl.addDiscarded(c);
						}
					} else {
						impl.addUnit(maxCollapsable);
						maxCollapsable = c;
					}
				} else {
					maxCollapsable = c;
				}
			} else {
				if (maxCollapsable!=null) {
					impl.addUnit(maxCollapsable);
					maxCollapsable = null;
				}
				impl.addUnit(c);
			}
			if (impl.overflows(maxCollapsable)) { //time to exit
				units--;
				return units;
			}
		}
		if (maxCollapsable!=null) {
			impl.addUnit(maxCollapsable);
			maxCollapsable = null;
		}
		return units;
	}

	static int forwardSkippable(SplitPointDataSource<? extends SplitPointUnit> data, final int pos) {
		SplitPointUnit c;
		int ret = pos;
		if (data.hasElementAt(ret) && !(c=data.get(ret)).isBreakable()) {
			ret++;
			while (data.hasElementAt(ret) && (c=data.get(ret)).isSkippable()) {
				if (c.isBreakable()) {
					return ret;
				} else {
					ret++;
				}
			}
			//have we passed last element?
			if (!data.hasElementAt(ret)) {
				return ret-1;
			} else {
				return pos;
			}
		} else {
			return ret;
		}
	}

	static <T extends SplitPointUnit> BreakPointScannerResult findBreakpointBefore(SplitPointDataSource<T> data, int strPos, SplitPointCost<T> cost) {
		BreakPointScannerResult res = new BreakPointScannerResult();
		res.bestBreakable = -1;
		res.bestSplitPoint = strPos;
		double currentCost = Double.MAX_VALUE;
		double currentBreakableCost = Double.MAX_VALUE;
		for (int index=0; index<=strPos; index++) {
			double c = cost.getCost(data, index, strPos);
			if (c<currentCost) { // this should always be true for the first unit
				res.bestSplitPoint = index;
				currentCost = c;
			}
			if (c<currentBreakableCost && data.get(index).isBreakable()) {
				res.bestBreakable = index;
				currentBreakableCost = c;
			}
		}
		return res;
	}
	
	private static class BreakPointScannerResult {
		int bestBreakable;
		int bestSplitPoint;
	}
	
	/**
	 * Returns true if the total size is less than or equal to the limit, false otherwise.
	 * 
	 * @param data the units
	 * @param limit the maximum width that is relevant to calculate
	 * @return returns the size 
	 */
	static <T extends SplitPointUnit> boolean fits(SplitPointDataSource<T> data, float limit) {
		return totalSize(data, limit)<=limit;
	}
	/**
	 * If the total size is less than the limit, the size is returned, otherwise a value greater
	 * than or equal to the limit is returned.
	 * 
	 * @param data the units
	 * @param limit the maximum width that is relevant to calculate
	 * @return returns the size 
	 */
	static <T extends SplitPointUnit> float totalSize(SplitPointDataSource<T> data, float limit) {
		float ret = 0;
		Set<String> ids = new HashSet<>();
		Supplements<T> map = data.getSupplements();
		boolean hasSupplements = false;
		// we check up to the limit and beyond by one element, to make sure that we check enough units
		for (int i=0; data.hasElementAt(i) && ret<=limit; i++) {
			T unit = data.get(i);
			List<String> suppIds = unit.getSupplementaryIDs();
			if (suppIds!=null) {
				for (String id : suppIds) {
					if (ids.add(id)) { //id didn't already exist in the list
						T item = map.get(id);
						if (item!=null) {
							if (!hasSupplements) {
								hasSupplements = true;
								ret += map.getOverhead();
							}
							ret += item.getUnitSize();
						}
					}
				}
			}
			//last unit?
			if (!data.hasElementAt(i+1)) {
				ret += unit.getLastUnitSize();
			} else {
				ret += unit.getUnitSize();
			}
		}
		return ret;
	}

}

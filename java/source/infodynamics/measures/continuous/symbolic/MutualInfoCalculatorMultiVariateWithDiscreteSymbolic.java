package infodynamics.measures.continuous.symbolic;

import infodynamics.measures.continuous.MutualInfoCalculatorMultiVariateWithDiscrete;
import infodynamics.measures.discrete.MutualInformationCalculator;
import infodynamics.utils.FirstIndexComparatorDouble;
import infodynamics.utils.MathsUtils;
import infodynamics.utils.MatrixUtils;
import infodynamics.utils.EmpiricalMeasurementDistribution;
import infodynamics.utils.RandomGenerator;

public class MutualInfoCalculatorMultiVariateWithDiscreteSymbolic implements
		MutualInfoCalculatorMultiVariateWithDiscrete {

	// The calculator used to do the grunt work
	protected MutualInformationCalculator miCalc;
	
	protected int dimensions;
	
	protected int[][] permutations;
	// For each permutation index, holds the unique permutation id
	protected int[] permutationIds;
	// For each possible permutation id, holds the permutation index 
	protected int[] idToPermutationIndex;
	
	// Array indices for the 2D array sorted by the first index
	protected static final int VAL_COLUMN = 0;
	protected static final int VAR_NUM_COLUMN = 1;

	public static final String PROP_NORMALISE = "NORMALISE";
	private boolean normalise = true;

	public MutualInfoCalculatorMultiVariateWithDiscreteSymbolic() {
		// Nothing to do
	}

	public void initialise(int dimensions, int base) throws Exception {
		this.dimensions = dimensions;

		// First work out how many permutations of orderings we could have
		RandomGenerator rg = new RandomGenerator();
		permutations = rg.generateAllDistinctPerturbations(dimensions);
		// Now generate an int signature for each permutation:
		permutationIds = new int[permutations.length];
		for (int r = 0; r < permutations.length; r++) {
			permutationIds[r] = generatePermutationId(permutations[r]);
		}
		// Now we have a list of permutations, each with an ID (which will be dimensions^dimensions)
		// Generate a reverse mapping from permutation identifier to permutation id
		idToPermutationIndex = new int[MathsUtils.power(dimensions, dimensions)];
		// First initialise all mappings to -1 : this will force an Array lookup error if 
		//  an identifier is not calculated correctly (most of the time)
		for (int i = 0; i < idToPermutationIndex.length; i++) {
			idToPermutationIndex[i] = -1;
		}
		for (int idIndex = 0; idIndex < permutationIds.length; idIndex++) {
			idToPermutationIndex[permutationIds[idIndex]] = idIndex;
		}
		
		// so we have permutationIds.length permutations of order of the continuous variables
		//  and base possiblities of the discrete variable.
		// Select the base for MI as the max between these (since our only MI calc at the
		//  moment only handles one base)
		int baseToUse = Math.max(permutationIds.length, base);
		
		// Make the base the maximum of the number of combinations of orderings of the
		//  continuous variables and the discrete base.
		miCalc = new MutualInformationCalculator(baseToUse,0);
		miCalc.initialise();
	}

	/**
	 * Generate the unique permutation id for this permutation.
	 * 
	 * @param data
	 * @return
	 */
	private int generatePermutationId(int[] data) {
		int permutationId = 0;
		for (int c = 0; c < dimensions; c++) {
			permutationId *= dimensions;
			permutationId +=  data[c];
		}
		return permutationId;
	}

	/**
	 * Generate the unique permutation id for this permutation.
	 * Convert the floating point variable numbers into ints first
	 * 
	 * @param data
	 * @return
	 */
	private int generatePermutationId(double[] data) {
		int permutationId = 0;
		for (int c = 0; c < dimensions; c++) {
			permutationId *= dimensions;
			permutationId +=  (int) data[c];
		}
		return permutationId;
	}

	public void setProperty(String propertyName, String propertyValue) {
		if (propertyName.equals(PROP_NORMALISE)) {
			normalise = Boolean.parseBoolean(propertyValue);
		}
	}

	public void setObservations(double[][] continuousObservations,
			int[] discreteObservations) throws Exception {
		if (normalise) {
			// Normalise the continuous observations first
			continuousObservations = MatrixUtils.normaliseIntoNewArray(continuousObservations);
		}
		// Construct the orderings for the continuous observations
		int[] mappedPermutationIds = new int[continuousObservations.length];
		for (int t = 0; t < continuousObservations.length; t++) {
			// Work out what the order of the continuous variables was here:
			double[][] variablesAndIndices = new double[dimensions][2];
			for (int v = 0; v < dimensions; v++) {
				variablesAndIndices[v][VAL_COLUMN] = continuousObservations[t][v];
				variablesAndIndices[v][VAR_NUM_COLUMN] = v;
			}
			java.util.Arrays.sort(variablesAndIndices, FirstIndexComparatorDouble.getInstance());
			// Now the second column contains the order of values here
			double[] permutation = MatrixUtils.selectColumn(variablesAndIndices, VAR_NUM_COLUMN);
			int permutationId = generatePermutationId(permutation);
			mappedPermutationIds[t] = idToPermutationIndex[permutationId];
		}
		// Now we can set the observations
		miCalc.addObservations(mappedPermutationIds, discreteObservations);
	}

	public double computeAverageLocalOfObservations() throws Exception {
		return miCalc.computeAverageLocalOfObservations();
	}

	public double[] computeLocalUsingPreviousObservations(double[][] contStates,
			int[] discreteStates) throws Exception {
		throw new Exception("Local method not implemented yet");
	}

	public EmpiricalMeasurementDistribution computeSignificance(
			int numPermutationsToCheck) throws Exception {
		return miCalc.computeSignificance(numPermutationsToCheck);
	}

	public EmpiricalMeasurementDistribution computeSignificance(int[][] newOrderings)
			throws Exception {
		return miCalc.computeSignificance(newOrderings);
	}

	public void setDebug(boolean debug) {
		// miCalc.setDebug(debug);
	}

	public double getLastAverage() {
		return miCalc.getLastAverage();
	}

	public int getNumObservations() {
		return miCalc.getNumObservations();
	}

}

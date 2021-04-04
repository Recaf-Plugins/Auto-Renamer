package me.coley.recaf.plugin.rename.analysis;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A minimal Naive-Bayes classifier.
 *
 * @param <C>
 * 		Category type.
 * @param <A>
 * 		Attribute type.
 *
 * @author Matt Coley
 */
public class Bayes<C, A> {
	private final Map<C, Map<A, Integer>> attrCountPerCategory = new HashMap<>();
	private final Map<A, Integer> attrCount = new HashMap<>();
	private final Map<C, Integer> categoryCount = new HashMap<>();
	private float pseudoCount = 1.0f;
	private float threshold = 0f;
	private C fallback;

	/**
	 * @param category
	 * 		Category of the attributes collection.
	 * @param attributes
	 * 		The attributes to categorize.
	 * @param weight
	 * 		The training entry weight.
	 */
	public void train(C category, A[] attributes, int weight) {
		// Register attribute counts occurrences overall and per category.
		Arrays.stream(attributes).forEach(attribute -> {
			attrCount.merge(attribute, weight, Integer::sum);
			attrCountPerCategory.computeIfAbsent(category, c -> new HashMap<>())
					.merge(attribute, weight, Integer::sum);
		});
		categoryCount.merge(category, weight, Integer::sum);
	}

	/**
	 * @param attributes
	 * 		The attributes to categorize.
	 *
	 * @return Most probable category for the attributes. If the difference in probabilities
	 * between the two most likely options is not greater than the {@link #threshold} then
	 * {@link #fallback} is returned.
	 */
	public C classify(A[] attributes) {
		if (categoryCount.size() < 2)
			throw new IllegalStateException("Must have at least two categories");
		if (fallback == null)
			throw new IllegalStateException("Must specify the fallback classification");
		// Map categories to wrappers to hold the category/probability.
		// Then sort them (wrappers sorted by probability / natural order)
		List<ProbabilityHolder<C, Object>> probabilities = categoryCount.keySet().stream()
				.map(category -> new ProbabilityHolder<>(category, probability(attributes, category)))
				.sorted()
				.collect(Collectors.toList());
		// Compare the two most likely options
		ProbabilityHolder<C, Object> first = probabilities.get(0);
		ProbabilityHolder<C, Object> second = probabilities.get(1);
		if (first.probability - second.probability > threshold)
			return first.category;
		// Threshold not met, we're not sure enough to assert a claim, so we return the fallback.
		return fallback;
	}

	/**
	 * <pre>P(C|A)</pre>
	 *
	 * @param attributes
	 * 		The attributes to use.
	 * @param category
	 * 		The category to check against.
	 *
	 * @return The probability that the given attributes belong to the given category.
	 */
	private float probability(A[] attributes, C category) {
		int categories = categoryCount.keySet().size();
		int categoryFreq = categoryCount.getOrDefault(category, 0);
		int total = categoryCount.values().stream().reduce(0,(a, b) -> a + b);
		float categoryOccurrence = (float) categoryFreq / total;
		// Calculate product of all attribute probabilities: P(A..|C)
		float probability = 1.0f;
		for(A attribute : attributes) {
			// Count attribute occurrences
			// - Skip if no count for attribute
			int count = attrCount.getOrDefault(attribute, 0);
			if(count == 0)
				continue;
			// Count attribute occurrences per the given category
			// - Probability -> 0: No counts for category
			Map<A, Integer> attrCountPerCat = attrCountPerCategory.get(category);
			if(attrCountPerCat == null) {
				probability = 0;
				break;
			}
			int categoryCount = attrCountPerCat.getOrDefault(attribute, 0);
			// Compute independent probability fot the current attribute
			float ratio = (float) categoryCount / count;
			probability *= (pseudoCount + count * ratio) / (categories + count);
		}
		// Compute: P(A...|C) * P(C)
		return probability * categoryOccurrence;
	}

	/**
	 * @param pseudoCount
	 * 		The bayesian pseudo-count, see {@link #getPseudoCount()}.
	 */
	public void setPseudoCount(float pseudoCount) {
		this.pseudoCount = pseudoCount;
	}

	/**
	 * Used for small sample correction.
	 * Additionally prevents any probability from being 0.
	 *
	 * @return Bayesian pseudo-count modifier.
	 */
	public float getPseudoCount() {
		return pseudoCount;
	}

	/**
	 * @param threshold
	 * 		The difference threshold, see {@link #getThreshold()}.
	 */
	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	/**
	 * The required difference between the most likely and second most likely classification.
	 * If the difference is less than the threshold, the classification defaults to
	 * {@link #fallback}.
	 *
	 * @return Difference threshold.
	 */
	public float getThreshold() {
		return threshold;
	}

	/**
	 * @param fallback
	 * 		Default/fallback classification, see {@link #getFallback()}.
	 */
	public void setFallback(C fallback) {
		this.fallback = fallback;
	}

	/**
	 * Default classification value if the percent difference ({@link #getThreshold()}) is not met.
	 *
	 * @return Default/fallback classification.
	 */
	public C getFallback() {
		return fallback;
	}

	/**
	 * Wrapper for the category's computed probability.
	 *
	 * @param <C>
	 * 		Category type.
	 * @param <A>
	 * 		Attribute type.
	 */
	private static class ProbabilityHolder<C, A> implements Comparable<ProbabilityHolder<C, A>> {
		private static final DecimalFormat df = new DecimalFormat("##.##%");
		private final C category;
		private final float probability;

		private ProbabilityHolder(C category, float probability) {
			this.category = category;
			this.probability = probability;
		}

		@Override
		public int compareTo(ProbabilityHolder<C, A> o) {
			return Float.compare(o.probability, probability);
		}

		@Override
		public String toString() {
			return category + " -> " + df.format(probability);
		}
	}
}
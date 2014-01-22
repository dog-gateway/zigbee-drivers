/*
 * Dog - Utils
 * 
 * 
 * Copyright 2014 Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.drivers.zigbee.network.util;

import java.util.HashSet;
import java.util.Set;

/**
 * @author bonino
 *
 */
public class Sets
{
	/**
	 * TODO: check where to move this general utility method... Given to set of
	 * clusters (expressed as strings representing the cluster full names)
	 * computes the intersection between the 2 sets.
	 * 
	 * @param <T>
	 * 
	 * @param clusters1
	 * @param clusters2
	 * @return an {@link HashSet<String>} containing the sets
	 */
	public static <T> HashSet<T> getSingleSideIntersection(Set<T> clusters1,
			Set<T> clusters2)
	{
		// compute cluster intersection, initially 0
		HashSet<T> intersection = new HashSet<T>();

		// select the best cluster to iterate on
		Set<T> clustersToIterate = clusters1;
		Set<T> clustersToCheck = clusters2;
		if (clusters1.size() > clusters2.size())
		{
			clustersToIterate = clusters2;
			clustersToCheck = clusters1;
		}

		// iterate over the clusters (should be around O(n) where n is the
		// number of clusters in clustersToIterate)
		for (T cluster : clustersToIterate)
		{
			// if an intersection is found, add it to the resulting set
			if (clustersToCheck.contains(cluster))
				intersection.add(cluster);
		}
		return intersection;
	}
}

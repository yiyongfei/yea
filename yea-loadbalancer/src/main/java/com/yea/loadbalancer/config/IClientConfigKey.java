package com.yea.loadbalancer.config;

public interface IClientConfigKey<T> {

	@SuppressWarnings("rawtypes")
	public static final class Keys extends CommonClientConfigKey {
		private Keys(String configKey) {
			super(configKey);
		}
	}

	/**
	 * @return string representation of the key used for hash purpose.
	 */
	public String key();

	/**
	 * @return Data type for the key. For example, Integer.class.
	 */
	public Class<T> type();
}

FROM sebp/elk:es241_l240_k461
ADD ./03-tcp-input.conf /elk/config/03-tcp-input.conf
ADD ./30-output.conf /elk/config/30-output.conf
ADD ./12-kv.conf /elk/config/12-kv.conf
RUN ln -sf /elk/config/03-tcp-input.conf /etc/logstash/conf.d/03-tcp-input.conf
RUN ln -sf /elk/config/30-output.conf /etc/logstash/conf.d/30-output.conf
RUN ln -sf /elk/config/12-kv.conf /etc/logstash/conf.d/12-kv.conf

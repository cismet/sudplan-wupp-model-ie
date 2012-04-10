-- 
-- SELECT DropGeometryColumn('public', 'geocpm_point', 'geom');
-- SELECT DropGeometryColumn('public', 'geocpm_triangle', 'geom');
-- 
-- DROP TABLE geocpm_jt_manhole_triangle;
-- DROP TABLE geocpm_manhole;
-- DROP TABLE geocpm_jt_breaking_edge_triangle;
-- DROP TABLE geocpm_breaking_edge;
-- DROP TABLE geocpm_source_drain;
-- DROP TABLE geocpm_curve_value;
-- DROP TABLE geocpm_curve;
-- DROP TABLE geocpm_triangle;
-- DROP TABLE geocpm_point;
-- DROP TABLE geocpm_configuration;
-- 
-- DROP SEQUENCE geocpm_configuration_seq;
-- DROP SEQUENCE geocpm_breaking_edge_seq;

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_configuration (
    id INTEGER PRIMARY KEY,
    calc_begin TIMESTAMP,
    calc_end TIMESTAMP,
    write_node BOOLEAN,
    write_edge BOOLEAN,
    last_values BOOLEAN,
    save_marked BOOLEAN,
    merge_triangles BOOLEAN,
    min_calc_triangle_size NUMERIC(20,8),
    time_step_restriction BOOLEAN,
    save_velocity_curves BOOLEAN,
    save_flow_curves BOOLEAN,
    result_save_limit NUMERIC(20,8),
    number_of_threads INTEGER,
    q_in INTEGER,
    q_out INTEGER,
    geom INTEGER,
    dyna_form TEXT,
    geocpmi_d TEXT,
    geocpmf_d TEXT,
    geocpms_d TEXT,
    geocpm_ein_folder VARCHAR(50),
    dyna_ein_folder VARCHAR(50),


    FOREIGN KEY (geom) REFERENCES geom
);
-- we use the geom table of cids

-- this is for cids
CREATE SEQUENCE geocpm_configuration_seq MINVALUE 1 START 1;
ALTER TABLE geocpm_configuration ALTER COLUMN id SET DEFAULT nextval('geocpm_configuration_seq');

CREATE TABLE geocpm_point (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);
SELECT AddGeometryColumn('public', 'geocpm_point', 'geom', 31466, 'POINT', 3);

CREATE TABLE geocpm_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    geocpm_point_a_id BIGINT,
    geocpm_point_b_id BIGINT,
    geocpm_point_c_id BIGINT,
    neighbour_a_id INTEGER,
    neighbour_b_id INTEGER,
    neighbour_c_id INTEGER,
    roughness NUMERIC(18, 3),
    loss NUMERIC(18, 3),
    be_height_a NUMERIC(18, 3),
    be_height_b NUMERIC(18, 3),
    be_height_c NUMERIC(18, 3),
    marked BOOLEAN,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_point_a_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_b_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_c_id) REFERENCES geocpm_point
);
SELECT AddGeometryColumn('public', 'geocpm_triangle', 'geom', 31466, 'POLYGON', 3);

CREATE INDEX geocpm_triangle_index_idx ON geocpm_triangle  (index);

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_breaking_edge (
    id INTEGER PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    type INTEGER,
    height numeric(14, 2),
    triangle_count_high INTEGER,
    triangle_count_low INTEGER,
    geom INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geom) REFERENCES geom
);
-- we use the geom table of cids
--SELECT AddGeometryColumn('public', 'geocpm_breaking_edge', 'geom', 31466, 'LINESTRING', 2);

-- this is for cids
CREATE SEQUENCE geocpm_breaking_edge_seq MINVALUE 1 START 1;
ALTER TABLE geocpm_breaking_edge ALTER COLUMN id SET DEFAULT nextval('geocpm_breaking_edge_seq');

--CREATE SEQUENCE geocpm_breaking_edge_proxy_seq MINVALUE 1 START 1;
---- Creates the new table "geocpm_breaking_edge_proxy" with the columns "height NUMERIC NULL, geocpm_configuration_proxy INTEGER NULL, id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq')".
--CREATE TABLE geocpm_breaking_edge_proxy(
--    id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq'),
--    geocpm_configuration_proxy INTEGER, 
--    height NUMERIC(14,2),
--
--    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
--);


CREATE TABLE geocpm_jt_breaking_edge_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_breaking_edge_id INTEGER,
    geocpm_triangle_id BIGINT,
    orientation char(1),

    FOREIGN KEY (geocpm_breaking_edge_id) REFERENCES geocpm_breaking_edge,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);


CREATE TABLE geocpm_curve (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE TABLE geocpm_curve_value (
    id BIGSERIAL PRIMARY KEY,
    geocpm_curve_id BIGINT,
    t NUMERIC(18, 3),
    value NUMERIC(18, 3),

    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_source_drain (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),
    geocpm_triangle_id BIGINT,
    max_capacity NUMERIC(18, 3),
    geocpm_curve_id BIGINT,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle,
    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_manhole (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    internal_id INTEGER,
    cap_height NUMERIC(17, 2),
    free_leakage INTEGER,
    entry_profile NUMERIC(17, 2),
    loss_overfall NUMERIC(17, 2),
    loss_emersion NUMERIC(17, 2),
    length_emersion NUMERIC(17, 2),
    name VARCHAR(200),

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE TABLE geocpm_jt_manhole_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_manhole_id BIGINT,
    geocpm_triangle_id BIGINT,

    FOREIGN KEY (geocpm_manhole_id) REFERENCES geocpm_manhole,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);

-------------------------------------------------------------------------
------------- GeoCPM Model Output Import ---------------------------------------
-------------------------------------------------------------------------

CREATE TABLE geocpm_aus_info
(

  geocpm_configuration_id     BIGINT PRIMARY KEY,
  number_of_elements   BIGINT,                                     
  number_of_edges      BIGINT,                                          
  number_of_calc_steps BIGINT,                                           
  volume_drain_source           NUMERIC(10, 2),
  volume_street                 NUMERIC(10, 2), 
  volume_all                    NUMERIC(10, 2), 
  volume_loss                   NUMERIC(10, 2), 
  volume_exchange_dyna_geocpm   NUMERIC(10, 2), 
  volume_exchange_geocpm_dyna   NUMERIC(10, 2), 
  rain_surface_elements         NUMERIC(10, 2), 
  time_total                             NUMERIC(10, 2), 
  time_time_step_calc                    NUMERIC(10, 2),
  time_boundary_conditions               NUMERIC(10, 2), 
  time_boundary_conditions_source_drain  NUMERIC(10, 2), 
  time_boundary_conditions_manhole       NUMERIC(10, 2), 
  time_boundary_conditions_triangle      NUMERIC(10, 2),
  time_dgl                               NUMERIC(10, 2),
  time_overhead                          NUMERIC(10, 2)
);


CREATE TABLE geocpm_aus_max
(
  geocpm_configuration_id BIGINT,       
  geocpm_triangle_id      BIGINT,          
  water_level             NUMERIC(20, 10),

  PRIMARY KEY (geocpm_configuration_id, geocpm_triangle_id)
);


----------------------------------------------------------------
------- DELTA
----------------------------------------------------------------


CREATE SEQUENCE delta_breaking_edge_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 11
  CACHE 1;

CREATE SEQUENCE delta_configuration_delta_breaking_edge_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 11
  CACHE 1;


CREATE SEQUENCE delta_configuration_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 4
  CACHE 1;


CREATE TABLE delta_configuration
(
  description text,
  locked boolean DEFAULT false,
  name character varying(200),
  id integer NOT NULL DEFAULT nextval('delta_configuration_seq'::regclass),
  delta_breaking_edges integer,
  original_object integer,
  CONSTRAINT delta_configuration_pkey PRIMARY KEY (id )
);

CREATE TABLE delta_breaking_edge
(
  name character varying(200),
  height numeric(14,2),
  description text,
  id integer NOT NULL DEFAULT nextval('delta_breaking_edge_seq'::regclass),
  original_object integer,
  CONSTRAINT delta_breaking_edge_pkey PRIMARY KEY (id )
);

CREATE TABLE delta_configuration_delta_breaking_edge
(
  id integer NOT NULL DEFAULT nextval('delta_configuration_delta_breaking_edge_seq'::regclass),
  delta_configuration_reference integer,
  delta_breaking_edge integer NOT NULL,
  CONSTRAINT delta_configuration_delta_breaking_edge_pkey PRIMARY KEY (id )
);



--------------------------------
-- rainevent
--------------------------------

CREATE SEQUENCE rainevent_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rainevent_seq OWNER TO postgres;

--
-- TOC entry 283 (class 1259 OID 26684)
-- Dependencies: 3279 3280 6
-- Name: rainevent; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE rainevent (
    data text,
    name character varying(200) NOT NULL,
    id integer DEFAULT nextval('rainevent_seq'::regclass) NOT NULL,
    geom integer,
    forecast boolean DEFAULT false NOT NULL,
    description text,
    "interval" integer
);


ALTER TABLE public.rainevent OWNER TO postgres;
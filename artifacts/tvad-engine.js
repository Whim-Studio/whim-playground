/*
 * TVAD personality assessment — scoring/math engine (Task 2)
 *
 * Exposes exactly one global:
 *   window.TVADEngine = { validate, compute };
 *
 * Pure functions, no DOM. Consumes the data model from window.TVAD_DATA
 * (owned by Task 1) and a `responses` object keyed by 2-letter subtrait id
 * with integer values 1..5.
 */
(function (global) {
  'use strict';

  // ---- helpers ----------------------------------------------------------

  // Collect the 24 expected subtrait ids from the data model, in pole order.
  function expectedIds(data) {
    var ids = [];
    (data.axes || []).forEach(function (axis) {
      [axis.poleA, axis.poleB].forEach(function (pole) {
        (pole.subtraits || []).forEach(function (st) {
          ids.push(st.id);
        });
      });
    });
    return ids;
  }

  function isIntInRange(v, lo, hi) {
    return typeof v === 'number' && isFinite(v) && Math.floor(v) === v && v >= lo && v <= hi;
  }

  // Scale a single subtrait value (1..5) onto 0..100.
  function scaleSub(value) {
    return (value / 5) * 100;
  }

  // Scale a pole (sum of its 3 subtraits, 3..15) onto 0..100, rounded.
  function scalePole(sumOf3) {
    return Math.round((sumOf3 / 15) * 100);
  }

  // ---- validate ---------------------------------------------------------

  function validate(responses, data) {
    var ids = expectedIds(data);
    var missing = [];
    var invalid = [];
    responses = responses || {};

    ids.forEach(function (id) {
      var v = responses[id];
      if (v === undefined || v === null) {
        missing.push(id);
        return;
      }
      if (!isIntInRange(v, 1, 5)) {
        invalid.push(id);
      }
    });

    return { ok: missing.length === 0 && invalid.length === 0, missing: missing, invalid: invalid };
  }

  // ---- active-vector reference fn (spec) --------------------------------
  // Vactive(Vbase, Ps, Ef) = (Vbase*Ps) + (Ef*(100-Ps))
  // Included for spec completeness; does not affect ranking.
  function Vactive(Vbase, Ps, Ef) {
    return (Vbase * Ps) + (Ef * (100 - Ps));
  }

  // ---- compute ----------------------------------------------------------

  function poleResult(pole, responses, subScores) {
    var sum = 0;
    var subScoresMap = {};
    (pole.subtraits || []).forEach(function (st) {
      var raw = responses[st.id];
      var scaled = scaleSub(raw);
      sum += raw;
      subScoresMap[st.id] = scaled;
      subScores[st.id] = scaled; // accumulate into the global 24-id map
    });
    return {
      id: pole.id,
      name: pole.name,
      score: scalePole(sum),
      subScores: subScoresMap
    };
  }

  function compute(responses, data) {
    var subScores = {}; // all 24 scaled subtrait values

    var axes = (data.axes || []).map(function (axis) {
      var poleA = poleResult(axis.poleA, responses, subScores);
      var poleB = poleResult(axis.poleB, responses, subScores);
      var Ps = Math.abs(poleA.score - poleB.score);
      return {
        id: axis.id,
        name: axis.name,
        tension: axis.tension,
        description: axis.description,
        poleA: poleA,
        poleB: poleB,
        Ps: Ps,
        deadZone: Ps <= 10
      };
    });

    // Hierarchy: stable sort by Ps descending. Array.prototype.sort is stable
    // in modern engines; guard ties explicitly to preserve TVAD_DATA order.
    var ranked = axes
      .map(function (a, i) { return { a: a, i: i }; })
      .sort(function (x, y) {
        if (y.a.Ps !== x.a.Ps) return y.a.Ps - x.a.Ps;
        return x.i - y.i; // tie-break by original axis order
      })
      .map(function (w) { return w.a; });

    var primary = ranked[0];
    var auxiliary = ranked[1];

    var primaryDriver = primary
      ? { axisId: primary.id, name: primary.name, Ps: primary.Ps }
      : null;
    var auxiliaryValve = auxiliary
      ? { axisId: auxiliary.id, name: auxiliary.name, Ps: auxiliary.Ps }
      : null;

    // Composite — Crisis Leadership:
    // CL = ((Va + To + Dx)/3) * (1 + (Vt/100)) on 0..100 scaled values.
    var Va = subScores.Va, To = subScores.To, Dx = subScores.Dx, Vt = subScores.Vt;
    var clRaw = ((Va + To + Dx) / 3) * (1 + (Vt / 100));
    var crisisLeadership = Math.round(clRaw * 10) / 10; // 1 decimal

    var deadZones = axes
      .filter(function (a) { return a.deadZone; })
      .map(function (a) { return a.id; });

    return {
      axes: axes,
      ranked: ranked,
      primaryDriver: primaryDriver,
      auxiliaryValve: auxiliaryValve,
      subScores: subScores,
      composites: {
        crisisLeadership: { label: 'Crisis Leadership', value: crisisLeadership }
      },
      deadZones: deadZones
    };
  }

  global.TVADEngine = { validate: validate, compute: compute, Vactive: Vactive };

})(typeof window !== 'undefined' ? window : this);
